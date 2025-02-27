package org.insta.authentication.dao;

import org.insta.authentication.dao.querybuilder.QueryBuilder;
import org.insta.authentication.exception.*;
import org.insta.authentication.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.insta.databaseconnection.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Manage user accounts.
 * </p>
 *
 * <p>
 * This class implements the {@link UserAccountDAO} interface to provide functionality for managing user accounts, including creating, retrieving, updating, and deleting user profiles.
 * </p>
 *
 * <p>
 * It interacts with the database to perform operations related to user accounts and handles exceptions that may occur during these operations.
 * </p>
 *
 * <p>
 * The methods provided by this class allow for creating user profiles, updating profile information, retrieving profiles by ID, and deleting profiles.
 * </p>
 *
 * <p>
 * This class also contains methods to check for existing user credentials such as name, mobile number, and email address to avoid duplication.
 * </p>
 *
 * <p>
 * Singleton pattern is used to ensure that only one instance of this class is created throughout the application.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0 6 Feb 2024
 * @see QueryBuilder
 * @see UserAccountDAO
 * @see User
 * @see DatabaseConnection
 * @see ProfileCreationFailedException
 * @see ProfileUpdateFailedException
 * @see ProfileRetrivalFailedException
 * @see ProfileDeleteFailedException
 */
public final class UserAccountDAOImpl implements UserAccountDAO {

    private static final Logger LOGGER = LogManager.getLogger(UserAccountDAOImpl.class);
    private static UserAccountDAO userAccountDAOImpl;
    private final Connection connection;
    private final QueryBuilder queryBuilder;

    /**
     * <p>
     * Prevent instantiation from outside the class.
     * </p>
     */
    private UserAccountDAOImpl() {
        queryBuilder = QueryBuilder.getInstance();
        connection = DatabaseConnection.get();
    }

    /**
     * <p>
     * Returns the singleton instance of UserAccountDAOImpl.
     * </p>
     *
     * @return Singleton instance of UserAccountDAOImpl.
     */
    public static UserAccountDAO getInstance() {
        return userAccountDAOImpl == null ? new UserAccountDAOImpl() : userAccountDAOImpl;
    }

    /**
     * <p>
     * Creates a user profile.
     * </p>
     *
     * @param user The {@link User} object containing the user data.
     * @return The ID of the created user profile.
     * @throws DatabaseOperationFailed If the profile creation operation fails due to a database error.
     */
    public Integer createProfile(final User user) {

        try (final PreparedStatement preparedStatement
                     = connection.prepareStatement(queryBuilder.getCreateAccountQuery()
                , Statement.RETURN_GENERATED_KEYS)) {

            connection.setAutoCommit(false);
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getMobileNumber());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPassword());

            if (preparedStatement.executeUpdate() > 0) {
                queryBuilder.setUserId(preparedStatement, user);
                connection.commit();
                return createAddress(user);
            }

            return null;
        } catch (SQLException exception) {
            connectionRollback(connection);
            throw new DatabaseOperationFailed("Database operation failed");
        }
    }

    /**
     * <p>
     * Creates a user address.
     * </p>
     *
     * @param user The {@link User} object containing the user data.
     * @return The ID of the created user address.
     */
    private int createAddress(final User user) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.getCreateAddressQuery())) {

            preparedStatement.setInt(1, user.getAddress().getDoorNumber());
            preparedStatement.setString(2, user.getAddress().getState());
            preparedStatement.setInt(3, user.getUserId());

            if (preparedStatement.executeUpdate() > 0) {
                connection.commit();
                return user.getUserId();
            }
        } catch (Exception ignored) {
            throw new ProfileCreationFailedException("Address creation failed");
        }
        return user.getUserId();
    }

    /**
     * <p>
     * Updates a user profile.
     * </p>
     *
     * @param receivedObject The {@link User} object containing the updated user data.
     * @return True if the user profile is successfully updated, otherwise false.
     * @throws ProfileUpdateFailedException If the profile update operation fails.
     */
    public boolean updateProfile(final User receivedObject) {
        if (receivedObject.getUserId() <= 0) return false;

        final User tableObject = getProfile(receivedObject.getUserId());
        final User updatedObject = createUpdatedObject(receivedObject, tableObject);

        try (final PreparedStatement preparedStatement = connection.prepareStatement(String.join("", "UPDATE account ",
                "SET name = ?, mobile = ?, email = ?, password = ? ",
                "WHERE id = ?"))) {

            connection.setAutoCommit(true);
            preparedStatement.setString(1, updatedObject.getName());
            preparedStatement.setString(2, updatedObject.getMobileNumber());
            preparedStatement.setString(3, updatedObject.getEmail());
            preparedStatement.setString(4, updatedObject.getPassword());
            preparedStatement.setInt(5, updatedObject.getUserId());

            if (preparedStatement.executeUpdate() > 0) {
                return true;
            }
        } catch (SQLException sqlException) {
            throw new ProfileCreationFailedException("Profile creation failed");
        }
        return false;
    }

    /**
     * <p>
     * Updates a user profile.
     * </p>
     *
     * @param receivedObject The {@link User} object containing the updated user data.
     * @param tableObject    Refers the current record in the storable.
     * @return True if the user profile is successfully updated, otherwise false.
     * @throws ProfileUpdateFailedException If the profile update operation fails.
     */
    public User createUpdatedObject(final User receivedObject, final User tableObject) {
        if (!Objects.isNull(receivedObject.getName())) tableObject.setName(receivedObject.getName());
        if (!Objects.isNull(receivedObject.getEmail())) tableObject.setEmail(receivedObject.getEmail());
        if (!Objects.isNull(receivedObject.getMobileNumber()))
            tableObject.setMobileNumber(receivedObject.getMobileNumber());
        if (!Objects.isNull(receivedObject.getPassword())) tableObject.setPassword(receivedObject.getPassword());

        return receivedObject;
    }

    /**
     * <p>
     * Retrieves a user profile by ID.
     * </p>
     *
     * @param id The ID of the user profile to retrieve.
     * @return The user profile if found, null otherwise.
     * @throws ProfileRetrivalFailedException If the profile retrieval operation fails.
     */
    public User getProfile(final int id) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.getProfileQuery())) {

            connection.setAutoCommit(true);
            preparedStatement.setInt(1, id);

            final ResultSet resultSet = preparedStatement.executeQuery();

            return queryBuilder.setUser(resultSet);

        } catch (SQLException exception) {
            throw new ProfileRetrivalFailedException("Profile retrival failed");
        }
    }

    /**
     * <p>
     * Deletes a user profile by ID.
     * </p>
     *
     * @param id The ID of the user profile to delete.
     * @return True if the user profile is successfully deleted, otherwise false.
     * @throws ProfileDeleteFailedException If the profile
     */
    public Boolean deleteProfile(final int id) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.getDeleteAccountQuery())) {

            connection.setAutoCommit(false);
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);

            if (preparedStatement.executeUpdate() > 0) {
                connection.commit();
                return true;
            }

            return false;
        } catch (SQLException sqlException) {
            connectionRollback(connection);
            throw new ProfileDeleteFailedException("Profile deletion failed");
        }
    }

    /**
     * <p>
     * Checks if the given name is already registered in the database.
     * </p>
     *
     * @param name The name to check.
     * @return {@code true} if the name is already registered, {@code false} otherwise.
     * @throws ProfileCreationFailedException If an error occurs during the database operation.
     */
    private boolean checkNameRegistered(final String name) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.checkNameExists()
                , Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, name);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                return resultSet.next();
            }
        } catch (SQLException sqlException) {
            throw new ProfileCreationFailedException("Profile Creation failed");
        }
    }

    /**
     * <p>
     * Checks if the given mobile number is already registered in the database.
     * </p>
     *
     * @param mobile The mobile number to check.
     * @return {@code true} if the mobile number is already registered, {@code false} otherwise.
     * @throws ProfileCreationFailedException If an error occurs during the database operation.
     */
    private boolean checkMobileRegistered(final String mobile) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.checkMobileExists()
                , Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, mobile);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                return resultSet.next();
            }
        } catch (SQLException sqlException) {
            throw new ProfileCreationFailedException("Profile creation failed");
        }
    }

    /**
     * <p>
     * Checks if the given email address is already registered in the database.
     * </p>
     *
     * @param email The email address to check.
     * @return {@code true} if the email address is already registered, {@code false} otherwise.
     * @throws ProfileCreationFailedException If an error occurs during the database operation.
     */
    private boolean checkEmailRegistered(final String email) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.checkEmailExists()
                , Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, email);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                return resultSet.next();
            }
        } catch (SQLException sqlException) {
            throw new ProfileCreationFailedException("Profile creation failed");
        }
    }

    /**
     * <p>
     * Generates a list of invalid credentials based on the provided user object.
     * </p>
     *
     * @param user The user object containing the credentials to check.
     * @return A list of strings representing invalid credentials.
     */
    public List<String> getCredentialsInvalidList(final User user) {
        final List<String> credentialsInvalidList = new ArrayList<>();

        if (checkNameRegistered(user.getName())) credentialsInvalidList.add("Name already registered");
        if (checkMobileRegistered(user.getMobileNumber())) credentialsInvalidList.add("Mobile already registered");
        if (checkEmailRegistered(user.getEmail())) credentialsInvalidList.add("Email already registered");

        return credentialsInvalidList;
    }

    /**
     * <p>
     * Rolls back the database connection in case of a failed account creation.
     * </p>
     *
     * @param connection The database connection to roll back.
     * @throws ProfileCreationFailedException If rolling back the connection fails.
     */
    private void connectionRollback(final Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException sqlException) {
            throw new ProfileCreationFailedException("Roll back failed at account creation");
        }
    }
}
