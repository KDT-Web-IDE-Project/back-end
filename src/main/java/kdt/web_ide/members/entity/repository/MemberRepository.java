package kdt.web_ide.members.entity.repository;

import kdt.web_ide.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {


    Optional<Member> findByNickName(String nickName);


    Optional<Member> findByLoginId(String userId);

    @Query("""
    Select br.member
    From BoardUser br
    Where br.board.id=:boardId
    """)
    List<Member> findMemberListByBoardId(Long boardId);
}
