package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingModuleAnnotationTest {

    @Test
    fun `reports Single without Module annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            class MyServices {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows Single with Module annotation`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyServices {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Factory without Module annotation`() {
        val code = """
            import org.koin.core.annotation.Factory

            class MyServices {
                @Factory
                fun createService(): Service = ServiceImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows class without Koin annotations`() {
        val code = """
            class MyServices {
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Module without ComponentScan or includes or definitions`() {
        val code = """
            import org.koin.core.annotation.Module

            @Module
            class EmptyModule // No @ComponentScan, no includes, no definitions!
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("empty")
    }

    @Test
    fun `allows Module with ComponentScan`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan
            class MyModule
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Module with includes parameter`() {
        val code = """
            import org.koin.core.annotation.Module

            @Module(includes = [OtherModule::class])
            class MyModule
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Scoped without Module annotation`() {
        val code = """
            import org.koin.core.annotation.Scoped

            class MyServices {
                @Scoped
                fun provideService(): Service = ServiceImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows Module with internal definitions`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports class with mixed annotations where only some are Koin`() {
        val code = """
            import org.koin.core.annotation.Single

            class MyServices {
                @Deprecated("use new API")
                fun legacyMethod(): Unit = Unit

                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports Module with only non-Koin annotated methods as empty`() {
        val code = """
            import org.koin.core.annotation.Module

            @Module
            class MyModule {
                @Deprecated("old")
                fun legacyHelper(): Unit = Unit
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("empty")
    }

    @Test
    fun `allows Module with ActivityScope provider function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.android.annotation.ActivityScope
            import androidx.activity.ComponentActivity
            import androidx.metrics.performance.JankStats

            @Module
            class JankStatsKoinModule {
                @ActivityScope
                fun jankStats(activity: ComponentActivity): JankStats =
                    JankStats.createAndTrack(activity.window) {}
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Module with FragmentScope provider function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.android.annotation.FragmentScope

            @Module
            class FragmentModule {
                @FragmentScope
                fun provideFragmentHelper(): Helper = HelperImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Module with Configuration annotation`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Configuration

            @Module
            @Configuration
            class JankStatsKoinModule
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Module with Configuration and ActivityScope provider function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Configuration
            import org.koin.android.annotation.ActivityScope
            import androidx.activity.ComponentActivity
            import androidx.metrics.performance.JankStats

            @Module
            @Configuration
            class JankStatsKoinModule {
                @ActivityScope
                fun jankStats(activity: ComponentActivity): JankStats =
                    JankStats.createAndTrack(activity.window) {}
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
