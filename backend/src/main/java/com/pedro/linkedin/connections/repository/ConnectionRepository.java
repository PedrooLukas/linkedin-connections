package com.pedro.linkedin.connections.repository;

import com.pedro.linkedin.connections.domain.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByCompanyContainingIgnoreCase(String company);

    List<Connection> findByPositionContainingIgnoreCase(String position);

    List<Connection> findByCompanyContainingIgnoreCaseAndPositionContainingIgnoreCase(String company, String position);

    @Query("""
           select c.company as value, count(c) as total
           from Connection c
           where c.company is not null and trim(c.company) <> ''
           group by c.company
           order by count(c) desc, c.company asc
           """)
    List<ConnectionCountProjection> countByCompany();

    @Query("""
           select c.position as value, count(c) as total
           from Connection c
           where c.position is not null and trim(c.position) <> ''
           group by c.position
           order by count(c) desc, c.position asc
           """)
    List<ConnectionCountProjection> countByPosition();

    @Query("""
           select c from Connection c
           where (:company is null or lower(c.company) like lower(concat('%', :company, '%')))
             and (:positionKeyword is null or lower(c.position) like lower(concat('%', :positionKeyword, '%')))
             and (
                   :recruitersOnly = false or
                   lower(c.position) like '%recruiter%' or
                   lower(c.position) like '%talent acquisition%' or
                   lower(c.position) like '%human resources%' or
                   lower(c.position) like '%people partner%' or
                   lower(c.position) like '%hr%'
             )
           order by c.company asc nulls last, c.position asc nulls last, c.lastName asc, c.firstName asc
           """)
    List<Connection> findStrategicContacts(
            @Param("company") String company,
            @Param("positionKeyword") String positionKeyword,
            @Param("recruitersOnly") boolean recruitersOnly
    );
}
