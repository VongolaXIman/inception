/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.conceptlinking.service;

import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface ConceptLinkingService
{

    /**
     * Given a mention in the text, this method returns a list of ranked candidate entities
     * generated from a Knowledge Base.
     *
     * The candidates are retrieved in two separate queries, because of the higher number of results
     * returned by full-text matching, which are filtered first. To not possible lose any of the
     * candidates from the exact matching results, the latter are added to the ranking afterwards
     * and given top priority.
     *
     * @param aKB
     *            the KB used to generate candidates.
     * @param aTypedString
     *            What the user has typed so far in the text field. Might be null.
     * @param aMention
     *            Marked Surface form of an entity to be linked.
     * @param aMentionBeginOffset
     *            the offset where the mention begins in the text.
     * @param aJcas
     *            used to extract information about mention sentence tokens.
     * @return a ranked list of entities.
     */
    List<KBHandle> disambiguate(KnowledgeBase aKB, String aConceptScope,
            ConceptFeatureValueType aValueType, String aTypedString, String aMention,
            int aMentionBeginOffset, JCas aJcas);

    /**
     * Get all linking instances within the scope of a given knowledge base. If null is passed for
     * aRepositoryId, all enabled knowledge bases in the project are considered. If the given
     * aRepositoryId can not be found the method will return an empty list. This method calls
     * {@link #disambiguate} to determine the instances
     * for a single knowledge base
     *
     * @param aRepositoryId
     *            the RepositoryId of the knowledge base that defines the scope
     * @param aTypedString
     *            What the user has typed so far in the text field. Might be null.
     * @param aMention
     *            Marked Surface form of an entity to be linked.
     * @param aMentionBeginOffset
     *            the offset where the mention begins in the text.
     * @param aJCas
     *            used to extract information about mention sentence tokens.
     * @param aProject
     *            the project where the knowledge bases are configured
     * @return all linking instances within the scope
     */
    List<KBHandle> getLinkingInstancesInKBScope(String aRepositoryId, String aConceptScope,
            ConceptFeatureValueType aValueType, String aTypedString,
            String aMention, int aMentionBeginOffset, JCas aJCas, Project aProject);
}
