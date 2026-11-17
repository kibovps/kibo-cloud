package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ArchivaDatabase
import com.example.data.model.FileCategory
import com.example.data.model.FileSource
import com.example.data.model.FileTypeClassifier
import com.example.data.repository.FileExplorerRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Kibo VPS Cloud`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Kibo VPS Cloud", context.getString(R.string.app_name))
    }

    @Test
    fun `file classifier recognizes multimedia and documents`() {
        assertEquals(FileCategory.VIDEO, FileTypeClassifier.classify("clip.mp4", null, false))
        assertEquals(FileCategory.AUDIO, FileTypeClassifier.classify("song.mp3", null, false))
        assertEquals(FileCategory.IMAGE, FileTypeClassifier.classify("photo.webp", null, false))
        assertEquals(FileCategory.PDF, FileTypeClassifier.classify("manual.pdf", null, false))
        assertEquals(FileCategory.TEXT, FileTypeClassifier.classify("notes.md", null, false))
        assertEquals(FileCategory.DOCUMENT, FileTypeClassifier.classify("report.docx", null, false))
        assertEquals(FileCategory.FOLDER, FileTypeClassifier.classify("Videos", null, true))
    }

    @Test
    fun `formatters format size and duration cleanly`() {
        assertEquals("0 B", Formatters.formatFileSize(0L))
        assertEquals("1.0 KB", Formatters.formatFileSize(1024L))
        assertEquals("01:05", Formatters.formatDuration(65_000L))
        assertEquals("1:01:05", Formatters.formatDuration(3_665_000L))
    }

    @Test
    fun `vault folder and text document lifecycle works`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ArchivaDatabase.getInstance(context)
        val repo = FileExplorerRepository(context, db.archivaDao())

        val rootUri = repo.vaultRootUri
        val initialItems = repo.listDirectory(rootUri, FileSource.VAULT).getOrThrow()
        assertTrue(initialItems.any { it.name == "Documentos" && it.isDirectory })

        repo.createFolder(rootUri, FileSource.VAULT, "Estudio").getOrThrow()
        val afterFolder = repo.listDirectory(rootUri, FileSource.VAULT).getOrThrow()
        val estudio = afterFolder.first { it.name == "Estudio" }

        val createdDoc = repo.createTextDocument(
            parentUriString = estudio.uriString,
            source = FileSource.VAULT,
            fileName = "resumen.md",
            content = "# Hola Archiva"
        ).getOrThrow()

        val (text, truncated) = repo.readTextContent(createdDoc.uriString).getOrThrow()
        assertEquals("# Hola Archiva", text)
        assertEquals(false, truncated)

        repo.renameItem(createdDoc, "resumen_final.md").getOrThrow()
        val estudioChildren = repo.listDirectory(estudio.uriString, FileSource.VAULT).getOrThrow()
        assertTrue(estudioChildren.any { it.name == "resumen_final.md" })

        val deleted = repo.deleteItems(listOf(estudio)).getOrThrow()
        assertEquals(1, deleted)
    }

    @Test
    fun `document file navigator lists and traverses nested subdirectories`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ArchivaDatabase.getInstance(context)
        val repo = FileExplorerRepository(context, db.archivaDao())

        val rootSegment = com.example.data.model.BreadcrumbSegment(
            name = "Bóveda Local",
            uriString = repo.vaultRootUri,
            source = FileSource.VAULT,
        )
        val sub = repo.navigator.createSubdirectory(rootSegment, "ProyectosNav")
        val subSegment = com.example.data.model.BreadcrumbSegment(
            name = "ProyectosNav",
            uriString = sub.uri.toString(),
            source = FileSource.VAULT,
            rootUriString = repo.vaultRootUri,
            pathSegments = listOf("ProyectosNav"),
        )
        repo.navigator.createSubdirectory(subSegment, "Subnivel")
        val listed = repo.listDirectory(subSegment).getOrThrow()
        assertTrue(listed.any { it.name == "Subnivel" && it.isDirectory })
        sub.delete()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `search query filters visible explorer items in real time by file name`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val app = ApplicationProvider.getApplicationContext<android.app.Application>()
            val vm = com.example.ui.viewmodel.MainViewModel(app)
            val rootUri = vm.repository.vaultRootUri
            vm.repository.createTextDocument(rootUri, FileSource.VAULT, "video_vacaciones.txt", "a").getOrThrow()
            vm.repository.createTextDocument(rootUri, FileSource.VAULT, "factura_abril.txt", "b").getOrThrow()
            vm.loadCurrentDirectory()

            val unfiltered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                withTimeout(5000) {
                    vm.explorerState.first { state ->
                        state.visibleItems.any { it.name == "factura_abril.txt" }
                    }
                }
            }
            assertTrue(unfiltered.visibleItems.any { it.name == "video_vacaciones.txt" })

            vm.setSearchQuery("factura")
            val filtered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                withTimeout(5000) {
                    vm.explorerState.first { state ->
                        state.searchQuery == "factura" && state.visibleItems.none { it.name == "video_vacaciones.txt" }
                    }
                }
            }
            assertTrue(filtered.visibleItems.any { it.name == "factura_abril.txt" })
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }
}
