package app.lasttime

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.lasttime.data.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scaffolding for schema changes: the exported schemas in app/schemas are packaged as test assets.
 * When the version is bumped, create the old version with [MigrationTestHelper.createDatabase], insert rows,
 * then call runMigrationsAndValidate with the new Migration and assert the rows survived.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun version1SchemaCreatesAndValidates() {
        helper.createDatabase("migration-test", 1).close()
        helper.runMigrationsAndValidate("migration-test", 1, true).close()
    }
}
