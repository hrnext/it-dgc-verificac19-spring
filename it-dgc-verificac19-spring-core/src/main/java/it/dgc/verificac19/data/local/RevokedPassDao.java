package it.dgc.verificac19.data.local;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RevokedPassDao extends JpaRepository<RevokedPass, String> {

    Optional<RevokedPass> findOneByHashedUVCI(String hashedUVCI);

    @Transactional
    void deleteAllByHashedUVCINotIn(List<String> hashedUVCI);

    @Transactional
    void deleteAllByHashedUVCIIn(List<String> deltaDeleteList);
}
