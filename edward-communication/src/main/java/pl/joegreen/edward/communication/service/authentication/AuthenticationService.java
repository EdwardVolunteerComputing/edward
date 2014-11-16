package pl.joegreen.edward.communication.service.authentication;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.User;
import pl.joegreen.edward.persistence.dao.UserDao;

@Component
public class AuthenticationService implements UserDetailsService {

	@Autowired
	private UserDao userDao;

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		User user = userDao.getByName(username);
		if (user == null) {
			return null;
		} else {
			return new org.springframework.security.core.userdetails.User(
					user.getName(), user.getPassword(),
					new ArrayList<GrantedAuthority>());
		}
	}
}
