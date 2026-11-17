DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM school_branding
        WHERE primary_color !~ '^#[0-9A-Fa-f]{6}$'
    ) THEN
        ALTER TABLE school_branding
            ADD CONSTRAINT school_branding_primary_color_check
            CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM school_branding
        WHERE secondary_color !~ '^#[0-9A-Fa-f]{6}$'
    ) THEN
        ALTER TABLE school_branding
            ADD CONSTRAINT school_branding_secondary_color_check
            CHECK (secondary_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM school_branding
        WHERE accent_color !~ '^#[0-9A-Fa-f]{6}$'
    ) THEN
        ALTER TABLE school_branding
            ADD CONSTRAINT school_branding_accent_color_check
            CHECK (accent_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM school_branding
        WHERE background_color !~ '^#[0-9A-Fa-f]{6}$'
    ) THEN
        ALTER TABLE school_branding
            ADD CONSTRAINT school_branding_background_color_check
            CHECK (background_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM school_branding
        WHERE text_color !~ '^#[0-9A-Fa-f]{6}$'
    ) THEN
        ALTER TABLE school_branding
            ADD CONSTRAINT school_branding_text_color_check
            CHECK (text_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;
END $$;
