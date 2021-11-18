package it.dgc.verificac19.data.local;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * This interface provides the methods that the rest of the app uses to interact with data in the
 * [Key] table.
 *
 */
@Repository
public interface KeyDao extends JpaRepository<Key, String> {

  void deleteAllByKidNotIn(List<String> keyIds);

}
