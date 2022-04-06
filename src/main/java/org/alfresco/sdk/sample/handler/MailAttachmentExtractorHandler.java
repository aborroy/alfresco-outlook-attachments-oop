/*
 * Copyright 2021-2021 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.sdk.sample.handler;

import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.model.AssociationBody;
import org.alfresco.core.model.NodeBodyCreate;
import org.alfresco.core.model.NodeEntry;
import org.alfresco.event.sdk.handling.filter.EventFilter;
import org.alfresco.event.sdk.handling.filter.IsFileFilter;
import org.alfresco.event.sdk.handling.filter.MimeTypeFilter;
import org.alfresco.event.sdk.handling.handler.OnNodeCreatedEventHandler;
import org.alfresco.event.sdk.model.v1.model.DataAttributes;
import org.alfresco.event.sdk.model.v1.model.NodeResource;
import org.alfresco.event.sdk.model.v1.model.RepoEvent;
import org.alfresco.event.sdk.model.v1.model.Resource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class MailAttachmentExtractorHandler implements OnNodeCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailAttachmentExtractorHandler.class);

    // Alfresco Content Model identifiers
    public static final String CM_FOLDER = "cm:folder";
    public static final String CM_CONTENT = "cm:content";
    public static final String IMAP_ATTACHMENT = "imap:attachment";
    public static final String IMAP_ATTACHMENTS_FOLDER = "imap:attachmentsFolder";

    @Autowired
    NodesApi nodesApi;

    @Override
    public void handleEvent(final RepoEvent<DataAttributes<Resource>> repoEvent) {

        final NodeResource nodeResource = (NodeResource) repoEvent.getData().getResource();

        LOGGER.info("An EMAIL content named {} has been created!", nodeResource.getName());

        try {

            LOGGER.info("Retrieving content from Alfresco node {}", nodeResource.getName());

            InputStream mailFileInputStream = nodesApi.getNodeContent(nodeResource.getId(), true, null, null)
                    .getBody()
                    .getInputStream();

            MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties(), null), mailFileInputStream);
            List<String> attachments = createAttachments(nodeResource.getId(), createAttachmentsFolder(nodeResource), message);

            LOGGER.info("Attachments " + attachments + " have been uploaded to Alfresco");

        } catch (Exception ex) {
            LOGGER.error("An error occurred trying to download the content of the file", ex);
        }
    }

    @Override
    public EventFilter getEventFilter() {
        return IsFileFilter.get()
                .and(MimeTypeFilter.of("message/rfc822"))
                .or(MimeTypeFilter.of("application/vnd.ms-outlook"));
    }

    private String createAttachmentsFolder(NodeResource nodeResource) {
        final NodeBodyCreate nodeBodyCreate = new NodeBodyCreate().nodeType(CM_FOLDER)
                .name(FilenameUtils.removeExtension(nodeResource.getName()));
        ResponseEntity<NodeEntry> responseEntity =
                nodesApi.createNode(nodeResource.getPrimaryHierarchy().get(0), nodeBodyCreate, true, null, null, null, null);
        String folderId = responseEntity.getBody().getEntry().getId();
        final AssociationBody associationBody = new AssociationBody().assocType(IMAP_ATTACHMENTS_FOLDER).targetId(folderId);
        nodesApi.createAssociation(nodeResource.getId(), associationBody, null);
        return folderId;
    }

    private List<String> createAttachments(String messageId, String folderId, Message message) throws IOException, MessagingException {
        List<String> attachments = new ArrayList<>();
        Multipart multiPart = (Multipart) message.getContent();
        int numberOfParts = multiPart.getCount();
        for (int partCount = 0; partCount < numberOfParts; partCount++) {
            MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                String file = part.getFileName();

                final NodeBodyCreate nodeBodyCreate = new NodeBodyCreate().nodeType(CM_CONTENT).name(file);
                ResponseEntity<NodeEntry> responseEntity =
                        nodesApi.createNode(folderId, nodeBodyCreate, true, null, null, null, null);
                nodesApi.updateNodeContent(responseEntity.getBody().getEntry().getId(), IOUtils.toByteArray(part.getInputStream()),
                        false, null, null, null, null);
                final AssociationBody associationBody = new AssociationBody().assocType(IMAP_ATTACHMENT).targetId(responseEntity.getBody().getEntry().getId());
                nodesApi.createAssociation(messageId, associationBody, null);
                attachments.add(file);
            }
        }
        return attachments;
    }

}
