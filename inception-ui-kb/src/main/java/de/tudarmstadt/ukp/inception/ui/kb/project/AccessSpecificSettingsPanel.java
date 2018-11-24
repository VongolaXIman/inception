package de.tudarmstadt.ukp.inception.ui.kb.project;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.TempFileResource;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.io.FileUploadDownloadHelper;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseAccess;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.ui.kb.project.wizard.KnowledgeBaseCreationWizard;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AccessSpecificSettingsPanel extends Panel
{
    private static final Logger log = LoggerFactory.getLogger(AccessSpecificSettingsPanel.class);

    private final IModel<Project> projectModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    // Remote
    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;
    private static final int MAXIMUM_REMOTE_REPO_SUGGESTIONS = 10;

    // Local
    private FileUploadField fileUploadField;
    private WebMarkupContainer listViewContainer;
    private KnowledgeBaseProfile selectedKnowledgeBaseProfile;
    private static final String CLASSPATH_PREFIX = "classpath:";
    private final Map<String, KnowledgeBaseProfile> downloadedProfiles;
    private final Map<String, File> uploadedFiles;


    private static final RDFFormat getRdfFormatForFileExt(String fileExt)
    {
        return EXPORT_FORMATS.stream().filter(f -> f.getDefaultFileExtension().equals(fileExt))
            .findAny().get();
    }
    private static final List<RDFFormat> EXPORT_FORMATS = Arrays
        .asList(RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.TURTLE);
    private static final List<String> EXPORT_FORMAT_FILE_EXTENSIONS = EXPORT_FORMATS.stream()
        .map(f -> f.getDefaultFileExtension()).collect(Collectors.toList());

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbproperties;

    public AccessSpecificSettingsPanel(String id, IModel<Project> aProjectModel,
        CompoundPropertyModel<KnowledgeBaseWrapper> aModel,
        Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(id);
        setOutputMarkupId(true);

        projectModel = aProjectModel;
        kbModel = aModel;
        knowledgeBaseProfiles = aKnowledgeBaseProfiles;
        downloadedProfiles = new HashMap<>();
        uploadedFiles = new HashMap<>();
        kbModel.getObject().setFiles(new ArrayList<>());

        boolean isHandlingLocalRepository =
            kbModel.getObject().getKb().getType() == RepositoryType.LOCAL;

        // container for form components related to local KBs
        WebMarkupContainer local = new WebMarkupContainer("localSpecificSettings");
        add(local);
        local.setVisibilityAllowed(isHandlingLocalRepository);
        setUpLocalSpecificSettings(local);

        // container for form components related to remote KBs
        WebMarkupContainer remote = new WebMarkupContainer("remoteSpecificSettings");
        add(remote);
        remote.setVisibilityAllowed(!isHandlingLocalRepository);
        setUpRemoteSpecificSettings(remote);
    }

    private void setUpRemoteSpecificSettings(WebMarkupContainer wmc) {
        // In case the user stepped back from the local mode
        kbModel.getObject().getKb().setFullTextSearchIri(null);

        RequiredTextField<String> urlField = new RequiredTextField<>("url");
        urlField.add(Validators.URL_VALIDATOR);
        wmc.add(urlField);

        // for up to MAXIMUM_REMOTE_REPO_SUGGESTIONS of knowledge bases, create a link which
        // directly fills in the URL field (convenient for both developers AND users :))
        List<KnowledgeBaseProfile> suggestions = new ArrayList<>(
            knowledgeBaseProfiles.values().stream()
                .filter(kb -> RepositoryType.REMOTE.equals(kb.getType()))
                .collect(Collectors.toList()));
        suggestions = suggestions.subList(0,
            Math.min(suggestions.size(), MAXIMUM_REMOTE_REPO_SUGGESTIONS));
        wmc.add(new ListView<KnowledgeBaseProfile>("suggestions", suggestions)
        {

            private static final long serialVersionUID = 4179629475064638272L;

            @Override
            protected void populateItem(ListItem<KnowledgeBaseProfile> item) {
                // add a link for one knowledge base with proper label
                LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", t -> {
                    // set all the fields according to the chosen profile
                    kbModel.getObject().setUrl(item.getModelObject().getAccess().getAccessUrl());
                    // sets root concepts list - if null then an empty list otherwise change the
                    // values to IRI and populate the list
                    kbModel.getObject().getKb().applyRootConcepts(item.getModelObject());
                    kbModel.getObject().getKb().applyMapping(item.getModelObject().getMapping());
                    t.add(urlField);
                });
                link.add(new Label("suggestionLabel", item.getModelObject().getName()));
                item.add(link);
            }
        });
    }

    private void setUpLocalSpecificSettings(WebMarkupContainer wmc) {
        fileUploadField = new FileUploadField("upload");
        wmc.add(fileUploadField);

        wmc.add(new LambdaAjaxLink("uploadButton", AccessSpecificSettingsPanel.this::actionUpload));

        // add link for clearing the knowledge base contents, enabled only, if there is
        // something to clear
        AjaxLink<Void> clearLink = new LambdaAjaxLink("clear", this::actionClear)
        {

            private static final long serialVersionUID = -6272361381689154558L;

            @Override public boolean isEnabled()
            {
                return true; //kbService.isEmpty(kbModel.getObject().getKb());
            }
        };
        wmc.add(clearLink);

        ListView<String> lv = new ListView<String>("exportButtons",
            EXPORT_FORMAT_FILE_EXTENSIONS)
        {

            private static final long serialVersionUID = -1869762759620557362L;

            @Override protected void populateItem(ListItem<String> item)
            {
                // creates an appropriately labeled {@link AjaxDownloadLink} which triggers the
                // download of the contents of the current KB in the given format
                String fileExtension = item.getModelObject();
                Model<String> exportFileNameModel = Model
                    .of(kbModel.getObject().getKb().getName() + "." + fileExtension);
                AjaxDownloadLink exportLink = new AjaxDownloadLink("link", exportFileNameModel,
                    LambdaModel
                        .of(() -> actionExport(fileExtension)));
                exportLink
                    .add(new Label("label", new ResourceModel("kb.export." + fileExtension)));
                item.add(exportLink);
            }
        };
        wmc.add(lv);

        List<KnowledgeBaseProfile> downloadableKBs = knowledgeBaseProfiles.values().stream()
            .filter(kb -> RepositoryType.LOCAL.equals(kb.getType()))
            .collect(Collectors.toList());

        listViewContainer = new WebMarkupContainer("listViewContainer");
        ListView<KnowledgeBaseProfile> suggestions = new ListView<KnowledgeBaseProfile>(
            "downloadableKBs", downloadableKBs)
        {
            private static final long serialVersionUID = 1L;

            @Override protected void populateItem(ListItem<KnowledgeBaseProfile> item)
            {
                LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", t -> {
                    selectedKnowledgeBaseProfile = item.getModelObject();
                });
                // Can not download the same KB more than once
                link.add(LambdaBehavior.onConfigure(_this -> setEnabled(
                    !downloadedProfiles.containsKey(item.getModelObject().getName()))));

                String itemLabel = item.getModelObject().getName();
                // Adjust label to indicate whether the KB has already been downloaded
                if (downloadedProfiles.containsKey(item.getModelObject().getName())) {
                    // &#10004; is the checkmark symbol
                    itemLabel = itemLabel + "  &#10004;";
                }
                link.add(new Label("suggestionLabel", itemLabel).setEscapeModelStrings(false));
                // Show schema type on mouseover
                link.add(AttributeModifier.append("title",
                    new StringResourceModel("kb.wizard.steps.local.schemaOnMouseOver", this)
                        .setParameters(
                            kbService.checkSchemaProfile(item.getModelObject()).getLabel(),
                            getAccessTypeLabel(item.getModelObject()))));
                item.add(link);
            }
        };
        suggestions.setOutputMarkupId(true);
        listViewContainer.add(suggestions);
        listViewContainer.setOutputMarkupId(true);
        wmc.add(listViewContainer);

        LambdaAjaxLink addKbButton = new LambdaAjaxLink("addKbButton",
            this::actionDownloadKbAndSetIRIs);
        addKbButton.add(new Label("addKbLabel", new ResourceModel("kb.wizard.steps.local.addKb")));
        wmc.add(addKbButton);
    }

    private void actionUpload(AjaxRequestTarget aTarget) {
        try {
            for (FileUpload fu : fileUploadField.getFileUploads()) {
                File tmp = uploadFile(fu);
                kbModel.getObject().getFiles().add(tmp);
            }
        }
        catch (Exception e) {
            log.error("Error while uploading files", e);
            error("Could not upload files");
        }
    }

    private File uploadFile(FileUpload fu) throws Exception
    {
        String fileName = fu.getClientFileName();
        if (!uploadedFiles.containsKey(fileName)) {
            FileUploadDownloadHelper fileUploadDownloadHelper = new FileUploadDownloadHelper(
                getApplication());
            File tmpFile = fileUploadDownloadHelper.writeFileUploadToTemporaryFile(fu, kbModel);
            uploadedFiles.put(fileName, tmpFile);
        }
        else {
            log.debug("File [{}] already downloaded, skipping!", fileName);
        }
        return uploadedFiles.get(fileName);
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        try {
            kbService.clear(kbModel.getObject().getKb());
            info(new StringResourceModel("kb.details.local.contents.clear.feedback",
                kbModel.bind("kb")));
            aTarget.add(this);
        }
        catch (RepositoryException e) {
            error(e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private IResourceStream actionExport(String rdfFormatFileExt)
    {
        return new TempFileResource((os) -> kbService
            .exportData(kbModel.getObject().getKb(), getRdfFormatForFileExt(rdfFormatFileExt), os));
    }

    private void actionDownloadKbAndSetIRIs(AjaxRequestTarget aTarget)
    {
        try {
            if (selectedKnowledgeBaseProfile != null) {

                String accessUrl = selectedKnowledgeBaseProfile.getAccess().getAccessUrl();

                if (!accessUrl.startsWith(CLASSPATH_PREFIX)) {
                    FileUploadDownloadHelper fileUploadDownloadHelper =
                        new FileUploadDownloadHelper(getApplication());
                    File tmpFile = fileUploadDownloadHelper
                        .writeFileDownloadToTemporaryFile(accessUrl, kbModel);
                    kbModel.getObject().getFiles().add(tmpFile);
                }
                else {
                    // import from classpath
                    File kbFile = kbService.readKbFileFromClassPathResource(accessUrl);
                    kbModel.getObject().getFiles().add(kbFile);
                }

                kbModel.getObject().getKb().applyMapping(
                    selectedKnowledgeBaseProfile.getMapping());
                downloadedProfiles
                    .put(selectedKnowledgeBaseProfile.getName(), selectedKnowledgeBaseProfile);
                aTarget.add(listViewContainer);
                selectedKnowledgeBaseProfile = null;
            }
        }
        catch (IOException e) {
            error("Unable to download or import knowledge base file " + e.getMessage());
            log.error("Unable to download or import knowledge base file ", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private String getAccessTypeLabel(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getAccess().getAccessUrl().startsWith(CLASSPATH_PREFIX)) {
            return "CLASSPATH";
        }
        else {
            return "DOWNLOAD";
        }
    }
}