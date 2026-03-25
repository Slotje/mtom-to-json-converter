package nl.belastingdienst.bte.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.belastingdienst.bte.config.YamlConfigService;
import nl.belastingdienst.bte.model.ClientConfig;
import nl.belastingdienst.bte.model.ValidationResult;
import nl.belastingdienst.bte.validation.SchemaValidator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

    @Inject
    YamlConfigService yamlConfigService;

    @Inject
    SchemaValidator schemaValidator;

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadConfig(InputStream yamlStream) {
        try {
            ClientConfig config = yamlConfigService.loadConfig(yamlStream);
            ValidationResult validation = schemaValidator.validate(config);

            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("validation", validation);
            response.put("success", validation.isValid());

            return Response.ok(response).build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Fout bij het laden van de YAML configuratie: " + e.getMessage());
            error.put("suggestion", "Controleer of het bestand geldige YAML syntax bevat");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    @POST
    @Path("/upload-text")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response uploadConfigText(String yamlContent) {
        try {
            ClientConfig config = yamlConfigService.loadConfigFromString(yamlContent);
            ValidationResult validation = schemaValidator.validate(config);

            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("validation", validation);
            response.put("success", validation.isValid());

            return Response.ok(response).build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Fout bij het laden van de YAML configuratie: " + e.getMessage());
            error.put("suggestion", "Controleer of het bestand geldige YAML syntax bevat");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    @GET
    public Response getConfig() {
        if (!yamlConfigService.hasConfig()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Geen configuratie geladen", "suggestion", "Upload eerst een YAML configuratie"))
                .build();
        }
        return Response.ok(yamlConfigService.getCurrentConfig()).build();
    }
}
