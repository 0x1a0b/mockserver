package org.mockserver.templates.engine.velocity;

import org.apache.velocity.script.VelocityScriptEngineFactory;
import org.mockserver.client.serialization.model.DTO;
import org.mockserver.logging.LogFormatter;
import org.mockserver.model.HttpRequest;
import org.mockserver.templates.engine.serializer.HttpTemplateOutputDeserializer;
import org.mockserver.templates.engine.TemplateEngine;
import org.mockserver.templates.engine.model.HttpRequestTemplateObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author jamesdbloom
 */
public class VelocityTemplateEngine implements TemplateEngine {

    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static final ScriptEngine engine;
    private static Logger logger = LoggerFactory.getLogger(VelocityTemplateEngine.class);
    private static LogFormatter logFormatter = new LogFormatter(logger);

    static {
        manager.registerEngineName("velocity", new VelocityScriptEngineFactory());
        engine = manager.getEngineByName("velocity");
    }

    private HttpTemplateOutputDeserializer httpTemplateOutputDeserializer = new HttpTemplateOutputDeserializer();

    @Override
    public <T> T executeTemplate(String template, HttpRequest httpRequest, Class<? extends DTO<T>> dtoClass) {
        try {
            Writer writer = new StringWriter();
            ScriptContext context = engine.getContext();
            context.setWriter(writer);
            context.setAttribute("request", new HttpRequestTemplateObject(httpRequest), ScriptContext.ENGINE_SCOPE);
            engine.eval(template);
            logFormatter.infoLog("Generated output:{}from template:{}for request:{}", writer.toString(), template, httpRequest);
            return httpTemplateOutputDeserializer.deserializer(writer.toString(), dtoClass);
        } catch (Exception e) {
            logFormatter.errorLog(e, "Exception transforming template:{}for request:{}", template, httpRequest);
        }
        return null;
    }
}
