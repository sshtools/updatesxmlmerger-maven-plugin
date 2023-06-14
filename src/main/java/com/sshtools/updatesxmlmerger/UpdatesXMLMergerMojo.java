package com.sshtools.updatesxmlmerger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Mojo(threadSafe = true, name = "updatesxmlmerger")
public class UpdatesXMLMergerMojo extends AbstractMojo {
	/**
	 * Location of output file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/media/updates.xml", property = "output")
	private File output;

	/**
	 * Location of the file.
	 */
	@Parameter(property = "input", required = true)
	private File[] input;

	@Override
	public void execute() throws MojoExecutionException {
		Document doc = null;

		try {
			for (var file : input) {
				try (var in = Files.newInputStream(file.toPath())) {
					var src = new InputSource(in);
					var newDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
					if (doc == null)
						doc = newDoc;
					else {
						if (!Objects.equals(getBaseUrl(doc), getBaseUrl(newDoc))) {
							throw new IOException("baseUrl differs between two inputs.");
						} else {
							var els = newDoc.getElementsByTagName("<entry>");
							for (int i = 0; i < els.getLength(); i++) {
								var el = els.item(i);
								var id = getTargetMediaFileId(el);

								var existing = findTargetMediaFileId(doc, id);
								if (existing == null) {
									doc.appendChild(el);
								} else
									getLog().info(MessageFormat.format(
											"Skipping media file {0} (with ID of {1}) because it already exists",
											getAttrVal(el, "fileName"), id));
							}
						}
					}
				}
			}
		} catch (SAXException | ParserConfigurationException | IOException ioe) {
			throw new MojoExecutionException("XML read and merge failed.", ioe);
		}

		if (doc == null)
			throw new MojoExecutionException("No input files.");

		try {
			var transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			var transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			try (var out = Files.newOutputStream(output.toPath())) {
				transformer.transform(new DOMSource(doc), new StreamResult(out));
			}
		} catch (TransformerException | IOException ioe) {
			throw new MojoExecutionException("XML transform and write failed.", ioe);
		}
	}

	private Node findTargetMediaFileId(Document doc, String id) throws IOException {

		var entries = doc.getElementsByTagName("entry");
		for (int i = 0; i < entries.getLength(); i++) {
			var entry = entries.item(i);
			var entryId = getTargetMediaFileId(entry);
			if (id.equals(entryId))
				return entry;
		}
		return null;
	}

	private String getTargetMediaFileId(Node el) throws IOException {
		var id = getAttrVal(el, "targetMediaFileId");
		if (id == null)
			throw new IOException("<entry> without a targetMediaFileId tag.");
		return id;
	}

	private String getAttrVal(Node el, String name) throws IOException {
		var attrs = el.getAttributes().getNamedItem(name);
		if (attrs == null)
			return null;
		else
			return attrs.getTextContent();
	}

	private String getBaseUrl(Document doc) throws IOException {
		var descs = doc.getElementsByTagName("updateDescriptor");
		if (descs.getLength() == 1) {
			var baseUrlEl = descs.item(0).getAttributes().getNamedItem("baseUrl");
			return baseUrlEl == null ? null : baseUrlEl.getTextContent();
		} else
			throw new IOException("Should be a single <updateDescriptor> tag.");
	}

}
