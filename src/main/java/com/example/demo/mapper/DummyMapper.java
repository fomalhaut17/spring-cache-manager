package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

// This is just a dummy mapper to ensure MyBatis initializes correctly.
// We can associate one of the caches with this mapper if needed for testing later.
@Mapper
public interface DummyMapper {
    int selectOne();
}
