# Contributing Guide

## Pull Requests

Pull requests (PRs) on GitHub are welcome under the Apache 2.0 license, see [COPYING](COPYING).

To submit a PR, do the following:

1. Create your own [fork](https://help.github.com/github/getting-started-with-github/fork-a-repo)
of freeotp-android project.
2. Make a branch with your changes (via the
[git command line](https://www.freecodecamp.org/forum/t/push-a-new-local-branch-to-a-remote-git-repository-and-track-it-too/13222)
or
[GitHub's interface](https://help.github.com/github/collaborating-with-issues-and-pull-requests/creating-and-deleting-branches-within-your-repository)).
3. Commit and push your changes to the branch in your fork, adhering to the
[commit message format](#commit-message-format).
4. Unless you have a large or complex PR,
[squash your changes](https://medium.com/@slamflipstrom/a-beginners-guide-to-squashing-commits-with-git-rebase-8185cf6e62ec)
into a single commit.
5. If your changes include updates to the user interface, attach [screenshots](#ui-changes) of
your new or updated screens as comments in the PR.

## Commit Message Format

Commits messages should adhere to the following structure:

```text
<title>
<BLANK LINE>
<body - optional>
<BLANK LINE>
<footer - optional>
```

1. `title` - a title for your commit. This should be less than 50 characters in length.
2. `body` - the body with details of your change. Each line should be less than 100 characters in
length.
3. `footer` - one or more of the following may be placed in the footer:
    1. If your change fixes an existing GitHub issue, reference it with `Fixes #<issue-number>`
    Mark each issue fixed on a separate line. 
    2. If your organization requires sign-offs, add your sign off line at the bottom of the footer.
    Ex: `Signed off by Jane Smith<jsmith@mycompany.com>`
    
A full example:

```text
Add Settings Screen

- Create new settings screen to manage user preferences
- Migrate existing configurations to user preferences

Fixes #23
Signed off by Jane Smith<jsmith@mycompany.com>
```

## UI Changes

If your PR creates or updates a screen, attach screen shots of your updates as a comment to the
pull request. Changes to existing screens should annotate the screen shot to highlight changed
elements.
