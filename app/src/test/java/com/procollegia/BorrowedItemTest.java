package com.procollegia;

import com.procollegia.adapters.BorrowedAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class BorrowedItemTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        BorrowedAdapter.BorrowedItem item = 
            new BorrowedAdapter.BorrowedItem("doc1", "Basketball", "🏀", "2026-04-04", "active");

        assertEquals("doc1", item.docId);
        assertEquals("Basketball", item.name);
        assertEquals("🏀", item.emoji);
        assertEquals("2026-04-04", item.borrowDate);
        assertEquals("active", item.status);
    }
}
