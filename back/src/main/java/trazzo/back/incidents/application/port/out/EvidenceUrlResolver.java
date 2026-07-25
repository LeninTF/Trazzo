package trazzo.back.incidents.application.port.out;

public interface EvidenceUrlResolver {
    String buildPublicUrl(String fileKey);
}
