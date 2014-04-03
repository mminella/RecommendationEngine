/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.springoverflow.service;

import io.spring.springoverflow.domain.User;
import io.spring.springoverflow.domain.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

/**
 * @author Michael Minella
 */
public class SpringOverflowUserDetailsService implements UserDetailsService {

	private UserRepository userRepository;

	public SpringOverflowUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByDisplayName(username);

		if(user != null) {
			return new org.springframework.security.core.userdetails.User(username, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_AUTHENTICATED")));
		} else {
			throw new UsernameNotFoundException("Unable to find " + username);
		}
	}
}
