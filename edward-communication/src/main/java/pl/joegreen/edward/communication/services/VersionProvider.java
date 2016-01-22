package pl.joegreen.edward.communication.services;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VersionProvider {
    public String getVersion() {
        return Optional.ofNullable(VersionProvider.class.getPackage().getImplementationVersion())
                .orElse("Development Version");
    }

}
