package org.insta.wrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * Bundle activator class for handling bundle start and stop events.
 * </p>
 *
 * <p>
 * This activator class implements the {@link BundleActivator} interface, which defines
 * methods to start and stop the bundle. In this implementation, the start and stop methods
 * simply print messages indicating the bundle's lifecycle events related to the "WRAPPER" bundle.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0 6 Feb 2024
 * @see BundleActivator
 */
public final class Activator implements BundleActivator {

    private static final Logger LOGGER = LogManager.getLogger(Activator.class);

    /**
     * <p>
     * Called when the bundle is started.
     * </p>
     *
     * @param context {@link BundleContext} The context of the bundle.
     */
    @Override
    public void start(final BundleContext context) {
        LOGGER.info("Wrapkit bundle started successfully");
    }

    /**
     * <p>
     * Called when the bundle is stopped.
     * </p>
     *
     * @param context {@link BundleContext} The context of the bundle.
     */
    @Override
    public void stop(final BundleContext context) {
        LOGGER.info("Wrapkit bundle stopped successfully");
    }
}
