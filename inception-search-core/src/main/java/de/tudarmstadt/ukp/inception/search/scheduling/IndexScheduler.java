/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.scheduling;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.ReindexTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.Task;

/**
 * Indexer scheduler. Does the project re-indexing in an asynchronous way.
 */
@Component
public class IndexScheduler
    implements InitializingBean, DisposableBean
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired ApplicationContext applicationContext;

    private Thread consumer;
    private BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);

    @Override
    public void afterPropertiesSet()
    {
        consumer = new Thread(new TaskConsumer(applicationContext, queue), "Index task consumer");
        consumer.setPriority(Thread.MIN_PRIORITY);
        consumer.start();
        log.info("Started Search Indexing Thread");
    }

    @Override
    public void destroy()
    {
        consumer.interrupt();
    }

    public void enqueueReindexTask(Project aProject)
    {
        // Add reindex task
        enqueue(new ReindexTask(aProject));
    }

    public void enqueueIndexDocument(SourceDocument aSourceDocument, JCas aJCas)
    {
        // Index source document
        enqueue(new IndexSourceDocumentTask(aSourceDocument, aJCas));
    }

    public void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, JCas aJCas)
    {
        // Index annotation document
        enqueue(new IndexAnnotationDocumentTask(aAnnotationDocument, aJCas));
    }
    
    /**
     * Put a new indexing task in the queue.
     * Indexing tasks can be of three types:
     *  - Indexing of a whole project
     *  - Indexing of a source document
     *  - Indexing of an annotation document for a given user
     *  
     * @param aRunnable
     *          The indexing task
     */
    public synchronized void enqueue(Task aRunnable)
    {
        Optional<Task> alreadyScheduledTask = findAlreadyScheduled(aRunnable);
        
        // Project indexing task
        if (aRunnable instanceof ReindexTask) {
            if (alreadyScheduledTask.isPresent()) {
                log.debug("Matching project indexing task already scheduled: [{}] - skipping ...",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.info("Enqueued new project indexing task: {}", aRunnable);
            }
        }
        // Source document indexing task
        else if (aRunnable instanceof IndexSourceDocumentTask) {
            if (alreadyScheduledTask.isPresent()) {
                log.debug(
                        "Matching source document indexing task already scheduled: [{}] - skipping ...",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.info("Enqueued new source document indexing task: {}", aRunnable);
            }
        }
        // Annotation document indexing task
        else if (aRunnable instanceof IndexAnnotationDocumentTask) {
            // Try to update the document CAS in the task currently enqueued for the same 
            // annotation document/user (if there is an enqueued task).
            // This must be done so that the task will take into account the
            // latest changes to the annotation document.
            if (alreadyScheduledTask.isPresent()) {
                alreadyScheduledTask.get().setJCas(aRunnable.getJCas());
                log.debug(
                        "Matching source document indexing task already scheduled: [{}] - updating CAS",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.info("Enqueued new annotation document indexing task: {}", aRunnable);
            }
        }
    }

    public synchronized void stopAllTasksForUser(String username)
    {
        Iterator<Task> taskIterator = queue.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();
            if (task.getUser().equals(username)) {
                queue.remove(task);
            }
        }
    }

    private Optional<Task> findAlreadyScheduled(Task aTask)
    {
        return queue.stream().filter(aTask::matches).findAny();
    }
}
