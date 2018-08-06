package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.workspace.git.GitRepo
import com.atlassian.performance.tools.workspace.git.LocalGitRepo
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File

class GitRepoTest {

    @Test
    fun shouldFindRepo() {
        val repo = GitRepo.findFromCurrentDirectory()

        assertThat(repo, notNullValue())
    }

    @Test
    fun shouldPrintBranchHead() {
        val testResource = "on-branch.git"
        val repo = getTestRepo(testResource)

        val head = repo.getHead()

        assertThat(head, equalTo("8b1171cbf2f51c76e4da0dd9ff563761b1f191ab"))
        println("Got Git branch HEAD: $head")
    }

    @Test
    fun shouldPrintBranchHeadForPackedRef() {
        val testResource = "on-branch-packed-refs.git"
        val repo = getTestRepo(testResource)

        val head = repo.getHead()

        assertThat(head, equalTo("14f2d8ec89d8dd885f7b561dc6ec823e402d551e"))
        println("Got Git branch HEAD: $head")
    }

    @Test
    fun shouldPrintCommitHead() {
        val repo = getTestRepo("on-commit.git")

        val head = repo.getHead()

        assertThat(head, equalTo("bc8578c511a558dbf542adc23fa1a33b24695c1f"))
        println("Got Git commit HEAD: $head")
    }

    private fun getTestRepo(
        testResource: String
    ): GitRepo = LocalGitRepo(
        FileRepository(
            File(
                javaClass.getResource(testResource).toURI()
            )
        )
    )
}