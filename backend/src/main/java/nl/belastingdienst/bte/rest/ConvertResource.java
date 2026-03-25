package nl.belastingdienst.bte.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.belastingdienst.bte.config.YamlConfigService;
import nl.belastingdienst.bte.converter.MtomToJsonConverter;
import nl.belastingdienst.bte.model.ConversionResult;
import nl.belastingdienst.bte.model.ValidationResult;
import nl.belastingdienst.bte.validation.BusinessRuleValidator;
import nl.belastingdienst.bte.validation.SchemaValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ConvertResource {

    @Inject
    MtomToJsonConverter converter;

    @Inject
    YamlConfigService yamlConfigService;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    BusinessRuleValidator businessRuleValidator;

    @POST
    @Path("/convert")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response convert(@HeaderParam("Content-Type") String contentType, byte[] mtomContent) {
        if (!yamlConfigService.hasConfig()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Geen configuratie geladen", "suggestion", "Upload eerst een YAML configuratie via /api/config/upload"))
                .build();
        }

        ConversionResult result = converter.convert(mtomContent, contentType, yamlConfigService.getCurrentConfig());
        return Response.ok(result).build();
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validate() {
        if (!yamlConfigService.hasConfig()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Geen configuratie geladen"))
                .build();
        }

        ValidationResult schemaResult = schemaValidator.validate(yamlConfigService.getCurrentConfig());
        ValidationResult businessResult = businessRuleValidator.validate(yamlConfigService.getCurrentConfig(), Collections.emptyMap());

        return Response.ok(Map.of(
            "validationResults", List.of(schemaResult, businessResult),
            "valid", schemaResult.isValid() && businessResult.isValid()
        )).build();
    }
}
