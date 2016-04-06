package pl.joegreen.edward.communication.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class LoggedUserNameProvider {
    public String getLoggedUserName(){
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        return principal.getName();
    }
}
