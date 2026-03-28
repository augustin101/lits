package com.example.lits.logic

interface LevelRepository {
    fun getLevelCount(size: Int): Int
    fun getLevel(size: Int, index: Int): Level?
}
