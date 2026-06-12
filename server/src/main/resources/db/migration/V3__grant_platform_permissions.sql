DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'water_dashboard'
    ) THEN
        GRANT USAGE ON SCHEMA platform TO water_dashboard;
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA platform TO water_dashboard;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA platform TO water_dashboard;
        GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA platform TO water_dashboard;

        ALTER DEFAULT PRIVILEGES IN SCHEMA platform
            GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO water_dashboard;
        ALTER DEFAULT PRIVILEGES IN SCHEMA platform
            GRANT USAGE, SELECT ON SEQUENCES TO water_dashboard;
        ALTER DEFAULT PRIVILEGES IN SCHEMA platform
            GRANT EXECUTE ON FUNCTIONS TO water_dashboard;
    END IF;
END;
$$;

