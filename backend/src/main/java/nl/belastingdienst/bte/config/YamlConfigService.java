package nl.belastingdienst.bte.config;

import jakarta.enterprise.context.ApplicationScoped;
import nl.belastingdienst.bte.model.ClientConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.InputStream;

@ApplicationScoped
public class YamlConfigService {

    private ClientConfig currentConfig;

    public ClientConfig loadConfig(InputStream yamlStream) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(ClientConfig.class, options));
        this.currentConfig = yaml.load(yamlStream);
        return this.currentConfig;
    }

    public ClientConfig loadConfigFromString(String yamlContent) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(ClientConfig.class, options));
        this.currentConfig = yaml.load(yamlContent);
        return this.currentConfig;
    }

    public ClientConfig getCurrentConfig() {
        return currentConfig;
    }

    public boolean hasConfig() {
        return currentConfig != null;
    }
}
