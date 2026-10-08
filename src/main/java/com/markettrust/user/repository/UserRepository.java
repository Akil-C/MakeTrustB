package com.markettrust.user.repository;

import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * Finds users filtered by status and role name with pagination support.
     *
     * @param status   the user account status
     * @param roleName the role to filter by
     * @param pageable pagination and sorting parameters
     * @return page of matching users
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
           "WHERE u.status = :status AND r.name = :roleName")
    Page<User> findByStatusAndRolesName(
            @Param("status") UserStatus status,
            @Param("roleName") RoleName roleName,
            Pageable pageable
    );

    /**
     * Full-text keyword search across name and email, optionally filtered by role.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
           "WHERE (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:roleName IS NULL OR r.name = :roleName) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleName") RoleName roleName,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
