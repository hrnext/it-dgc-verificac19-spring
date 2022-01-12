package it.dgc.verificac19.data.local;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrlDao extends JpaRepository<Drl, String> {

  Optional<Drl> findOneById(String id);

  Optional<Drl> findFirstByOrderById();

}
