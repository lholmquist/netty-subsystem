/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
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
package org.jboss.aerogear.netty.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class NettySubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(PathAddress.pathAddress(NettyExtension.SUBSYSTEM_PATH).toModelNode());
        list.add(subsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (!reader.getLocalName().equals(NettyExtension.SUBSYSTEM_NAME)) {
                throw ParseUtils.unexpectedElement(reader);
            }
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (reader.isStartElement()) {
                    readDeploymentType(reader, list);
                }
            }
        }
    }

    private void readDeploymentType(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode addTypeOperation = new ModelNode();
        addTypeOperation.get(OP).set(ModelDescriptionConstants.ADD);

        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attr = reader.getAttributeLocalName(i);
            final String value = reader.getAttributeValue(i);
            if (attr.equals(ServerDefinition.SOCKET_BINDING)) {
                ServerDefinition.SOCKET_BINDING_ATTR.parseAndSetParameter(value, addTypeOperation, reader);
            } else if (attr.equals(ServerDefinition.SERVER_NAME)) {
                name = value;
            }  else if (attr.equals(ServerDefinition.FACTORY_CLASS)) {
                ServerDefinition.FACTORY_CLASS_ATTR.parseAndSetParameter(value, addTypeOperation, reader);
            }  else if (attr.equals(ServerDefinition.THREAD_FACTORY)) {
                ServerDefinition.THREAD_FACTORY_ATTR.parseAndSetParameter(value, addTypeOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        if (name == null) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(ServerDefinition.SERVER_NAME));
        }

        final PathAddress addr = PathAddress.pathAddress(NettyExtension.SUBSYSTEM_PATH, PathElement.pathElement(NettyExtension.SERVER, name));
        addTypeOperation.get(OP_ADDR).set(addr.toModelNode());
        list.add(addTypeOperation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(NettyExtension.NAMESPACE, false);
        writer.writeStartElement(NettyExtension.SUBSYSTEM_NAME);
        final ModelNode node = context.getModelNode();
        final ModelNode type = node.get(NettyExtension.SERVER);
        for (Property property : type.asPropertyList()) {
            writer.writeStartElement(NettyExtension.SERVER);
            writer.writeAttribute(ServerDefinition.SERVER_NAME, property.getName());
            final ModelNode entry = property.getValue();
            ServerDefinition.SOCKET_BINDING_ATTR.marshallAsAttribute(entry, true, writer);
            ServerDefinition.FACTORY_CLASS_ATTR.marshallAsAttribute(entry, true, writer);
            ServerDefinition.THREAD_FACTORY_ATTR.marshallAsAttribute(entry, true, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }
}
