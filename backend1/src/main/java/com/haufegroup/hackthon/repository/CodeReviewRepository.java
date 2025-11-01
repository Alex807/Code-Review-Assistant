package com.haufegroup.hackthon.repository;

import com.haufegroup.hackthon.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
}
