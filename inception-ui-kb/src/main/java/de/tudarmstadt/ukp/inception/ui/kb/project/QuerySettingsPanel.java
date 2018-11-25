package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;

public class QuerySettingsPanel extends Panel {

    private final TextField<Integer> queryLimitField;
    private final CheckBox maxQueryLimitCheckBox;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    public QuerySettingsPanel(String id, IModel<Project> aProjectModel,
                              CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);
        setOutputMarkupId(true);

        kbModel = aModel;

        queryLimitField = queryLimitField("maxResults", kbModel.bind("kb.maxResults"));
        add(queryLimitField);
        maxQueryLimitCheckBox = maxQueryLimitCheckbox("maxQueryLimit", Model.of(false));
        add(maxQueryLimitCheckBox);
        add(ftsField("fullTextSearchIri", "kb.fullTextSearchIri"));

    }


    private CheckBox maxQueryLimitCheckbox(String id, IModel<Boolean> aModel)
    {
        return new AjaxCheckBox(id, aModel)
        {
            private static final long serialVersionUID = -8390353018496338400L;

            @Override
            public void onUpdate(AjaxRequestTarget aTarget)
            {
                if (getModelObject()) {
                    queryLimitField.setModelObject(kbProperties.getHardMaxResults());
                    queryLimitField.setEnabled(false);
                }
                else {
                    queryLimitField.setEnabled(true);
                }
                aTarget.add(queryLimitField);
            }
        };
    }

    private NumberTextField<Integer> queryLimitField(String id, IModel<Integer> aModel)
    {
        NumberTextField<Integer> queryLimit = new NumberTextField<>(id, aModel, Integer.class);
        queryLimit.setOutputMarkupId(true);
        queryLimit.setRequired(true);
        queryLimit.setMinimum(KnowledgeBaseProperties.HARD_MIN_RESULTS);
        queryLimit.setMaximum(kbProperties.getHardMaxResults());
        queryLimit.add(LambdaBehavior.onConfigure(it -> {
            // If not setting, initialize with default
            if (queryLimit.getModelObject() == null || queryLimit.getModelObject() == 0) {
                queryLimit.setModelObject(kbProperties.getDefaultMaxResults());
            }
            // Cap at local min results
            else if (queryLimit.getModelObject() < KnowledgeBaseProperties.HARD_MIN_RESULTS) {
                queryLimit.setModelObject(KnowledgeBaseProperties.HARD_MIN_RESULTS);
            }
            // Cap at local max results
            else if (queryLimit.getModelObject() > kbProperties.getHardMaxResults()) {
                queryLimit.setModelObject(kbProperties.getHardMaxResults());
            }
        }));
        return queryLimit;
    }

    private DropDownChoice<IRI> ftsField(String aId, String aProperty)
    {
        DropDownChoice<IRI> ftsField = new DropDownChoice<>(aId, kbModel.bind(aProperty),
            IriConstants.FTS_IRIS);
        ftsField.setOutputMarkupId(true);
        ftsField.setNullValid(true);
        return ftsField;
    }
}