package com.camunda.fox.cycle.web.service.resource;

import static org.fest.assertions.api.Assertions.*;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.camunda.fox.cycle.entity.User;
import com.camunda.fox.cycle.repository.UserRepository;
import com.camunda.fox.cycle.security.PrincipalHolder;
import com.camunda.fox.cycle.web.dto.UserDTO;
import com.sun.security.auth.UserPrincipal;

/**
 *
 * @author nico.rehwaldt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations = {"classpath:/spring/test-*.xml"}
)
public class UserServiceTest {

  @Inject
  private UserRepository userRepository;

  @Inject
  private UserService userService;

  @Test
  public void shouldCreateUser() throws Exception {
    // given
    UserDTO user = createUserDTO();

    // when
    UserDTO saved = userService.create(user);
    User databaseUser = userRepository.findById(saved.getId());

    // then
    assertThat(saved.getId()).isNotNull();

    // not publishing password (!!)
    assertThat(saved.getPassword()).isNull();

    // database user exists
    assertThat(databaseUser).isNotNull();

    assertThat(databaseUser.getName()).isEqualTo(user.getName());
    assertThat(databaseUser.getEmail()).isEqualTo(user.getEmail());
    assertThat(databaseUser.getPassword()).isEqualTo(user.getPassword());
  }

  @Test
  public void shouldGetUser() throws Exception {
    // given
    UserDTO user = createFlushedUserDTO();

    // when
    UserDTO gotUser = userService.get(user.getId());
    
    // then
    assertThat(gotUser.getId()).isNotNull();

    // database user exists
    assertThat(gotUser).isNotNull();

    assertThat(gotUser.getName()).isEqualTo(user.getName());
    assertThat(gotUser.getEmail()).isEqualTo(user.getEmail());
  }
  
  @Test
  public void shouldUpdateUserWithPassword() throws Exception {
    // given
    UserDTO user = createFlushedUserDTO();
    
    // when
    user.setPassword("blubs");
    
    UserDTO saved = userService.update(user);
    User databaseUser = userRepository.findById(saved.getId());
    
    // then
    assertThat(databaseUser).isNotNull();
    assertThat(databaseUser.getPassword()).isEqualTo(user.getPassword());
  }

  @Test
  public void shouldUpdateUserWithoutPassword() throws Exception {
    // given
    UserDTO user = createFlushedUserDTO();
    User originalDatabaseUser = userRepository.findById(user.getId());
    
    user.setName("Achim");
    user.setEmail("my@mail.com");
    
    // when
    UserDTO saved = userService.update(user);
    User afterUpdateDatabaseUser = userRepository.findById(saved.getId());
    
    // then
    assertThat(afterUpdateDatabaseUser).isNotNull();
    
    assertThat(afterUpdateDatabaseUser.getName()).isEqualTo(user.getName());
    assertThat(afterUpdateDatabaseUser.getEmail()).isEqualTo(user.getEmail());
    
    assertThat(afterUpdateDatabaseUser.getPassword()).isEqualTo(originalDatabaseUser.getPassword());
  }

  @Test
  public void shouldChangePassword() throws Exception {
    // given
    UserDTO user = createFlushedUserDTO();
    
    // assume principal set
    PrincipalHolder.setPrincipal(new UserPrincipal(user.getName()));
    
    // when
    userService.changePassword(user.getId(), "ASDF", "FOOBAR");
    User userAfterUpdate = userRepository.findById(user.getId());
    
    // then
    assertThat(userAfterUpdate.getPassword()).isEqualTo("FOOBAR");
  }

  @Test
  public void shouldNotChangePasswordOnWrongPrincipal() throws Exception {
    // given
    UserDTO user = createFlushedUserDTO();
    
    // assume principal set
    PrincipalHolder.setPrincipal(null);
    
    // when
    try {
      userService.changePassword(user.getId(), "ASDF", "FOOBAR");
      fail("expected exception");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(403);
    }
    
    User userAfterUpdateAttempt = userRepository.findById(user.getId());
    
    // then
    assertThat(userAfterUpdateAttempt.getPassword()).isEqualTo("ASDF");
  }

  @Test
  public void shouldNotChangePasswordOnMissingUser() throws Exception {
    try {
      userService.changePassword(-10, "ASDF", "FOOBAR");
      fail("expected exception");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(404);
    }
  }
  
  // Test data generation //////////////////////////////////////// 
  
  private UserDTO createUserDTO() {
    User user = createUser();
    return UserDTO.wrap(user);
  }

  private User createUser() {
    User user = new User();
    user.setName("Walter");
    user.setPassword("ASDF");
    user.setEmail("asdf@mail.com");
    
    return user;
  }

  private UserDTO createFlushedUserDTO() {
    User user = createUser();
    
    User savedUser = userRepository.saveAndFlush(user);
    return UserDTO.wrap(savedUser);
  }

  // Test initialization / cleanup /////////////////////////////////
  
  @After
  public void after() {
    // Remove all entities
    userRepository.deleteAll();
  }
}
