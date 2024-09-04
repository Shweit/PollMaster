package com.shweit.pollmaster.utils;

import com.shweit.pollmaster.PollMaster;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionManager {
    public Connection getConnection() throws SQLException {
        File dbFile = new File(PollMaster.getInstance().getDataFolder(), "polls.sqlite");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
    }
}
