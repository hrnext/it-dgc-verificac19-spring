package it.dgc.verificac19.data.local;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * This interface provides the methods that the rest of the app uses to interact with data in the
 * [Key] table.
 *
 */
@Repository
public interface BlackListDao extends JpaRepository<Blacklist, String> {

  Optional<Blacklist> findOneByBvalue(String bvalue);

}
