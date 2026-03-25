package nl.belastingdienst.bte.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.belastingdienst.bte.model.DetectedField;
import nl.belastingdienst.bte.parser.MtomFieldDetector;

import java.util.List;
import java.util.Map;

@Path("/api/mtom")
@Produces(MediaType.APPLICATION_JSON)
public class MtomAnalyzeResource {

    @Inject
    MtomFieldDetector fieldDetector;

    @POST
    @Path("/analyze")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response analyze(@HeaderParam("Content-Type") String contentType, byte[] mtomContent) {
        try {
            List<DetectedField> fields = fieldDetector.detectFields(mtomContent, contentType);
            return Response.ok(Map.of(
                "success", true,
                "fields", fields,
                "totalFields", fields.size()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                    "success", false,
                    "error", "Fout bij het analyseren van het MTOM bericht: " + e.getMessage(),
                    "suggestion", "Controleer of het bestand een geldig MTOM/MIME of XML bericht is"
                )).build();
        }
    }
}
