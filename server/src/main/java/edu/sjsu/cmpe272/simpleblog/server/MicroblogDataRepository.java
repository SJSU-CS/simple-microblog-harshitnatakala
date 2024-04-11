package edu.sjsu.cmpe272.simpleblog.server;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;



public interface MicroblogDataRepository extends JpaRepository<MicroblogPost, Long> {
    List<MicroblogPost> findAllByOrderByMessageIdDesc();
    List<MicroblogPost> findAllByOrderByMessageIdAsc();
}


