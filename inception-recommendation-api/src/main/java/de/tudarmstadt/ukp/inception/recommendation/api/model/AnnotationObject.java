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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnnotationObject
    implements Serializable
{
    private static final long serialVersionUID = -1145787227041121442L;

    private final int id;
    private final String uiLabel;
    private final String source;

    private final String documentName;
    private final String documentUri;

    private final int begin;
    private final int end;
    private final String coveredText;
    
    private final long recommenderId;
    private final String feature;
    private final String label;
    private final double confidence;
    
    private boolean visible = false;

    public AnnotationObject(String aDocumentName, String aDocumentUri, int aBegin, int aEnd,
            String aCoveredText, String aLabel, String aUiLabel, int aId, String aFeature,
            String aSource, double aConfidence, long aRecommenderId)
    {
        label = aLabel;
        uiLabel = aUiLabel;
        id = aId;
        feature = aFeature;
        source = aSource;
        confidence = aConfidence;
        recommenderId = aRecommenderId;
        begin = aBegin;
        end = aEnd;
        coveredText = aCoveredText;
        documentName = aDocumentName;
        documentUri = aDocumentUri;
    }

    /**
     * Copy constructor.
     *
     * @param aObject The annotationObject to copy
     */
    public AnnotationObject(AnnotationObject aObject)
    {
        label = aObject.label;
        uiLabel = aObject.uiLabel;
        id = aObject.id;
        feature = aObject.feature;
        source = aObject.source;
        confidence = aObject.confidence;
        recommenderId = aObject.recommenderId;
        begin = aObject.begin;
        end = aObject.end;
        coveredText = aObject.coveredText;
        documentName = aObject.documentName;
        documentUri = aObject.documentUri;
    }

    // Getter and setter

    public String getCoveredText()
    {
        return coveredText;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public int getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getUiLabel()
    {
        return uiLabel;
    }

    public String getFeature()
    {
        return feature;
    }

    public String getSource()
    {
        return source;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public Offset getOffset()
    {
        return new Offset(begin, end);
    }
    
    public void setVisible(boolean aVisible)
    {
        visible = aVisible;
    }

    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationObject that = (AnnotationObject) o;
        return id == that.id && recommenderId == that.recommenderId
            && documentName.equals(that.documentName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, recommenderId, documentName);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("id", id).append("label", label)
                .append("uiLabel", uiLabel).append("feature", feature).append("source", source)
                .append("confidence", confidence).append("recommenderId", recommenderId)
                .append("begin", begin).append("end", end).append("coveredText", coveredText)
                .append("documentName", documentName).append("documentUri", documentUri)
                .append("visible", visible).toString();
    }
}
