package com.edurite.student;
import com.edurite.upload.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;
class DocumentStorageTest {
    @TempDir Path directory;
    @Test void storesBytesAndRejectsTraversalWithoutOverwritingEarlierUploads() throws Exception {
        var storage = new StorageService(); ReflectionTestUtils.setField(storage, "directory", directory.toString());
        String first = storage.putObject("student-documents", "student/report.pdf", new byte[]{1,2,3});
        String next = storage.putObject("student-documents", "student/report.pdf", new byte[]{4,5});
        assertThat(next).isNotEqualTo(first);
        assertThat(storage.getObject(first)).containsExactly(1,2,3);
        assertThat(storage.getObject(next)).containsExactly(4,5);
        assertThatThrownBy(() -> storage.putObject("student-documents", "../outside", new byte[]{1})).isInstanceOf(IllegalArgumentException.class);
    }
}
