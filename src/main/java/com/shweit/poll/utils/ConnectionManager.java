package com.shweit.poll.utils;

import com.shweit.poll.Poll;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionManager {
    public Connection getConnection() throws SQLException {
        File dbFile = new File(Poll.getInstance().getDataFolder(), "polls.sqlite");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
    }
}
