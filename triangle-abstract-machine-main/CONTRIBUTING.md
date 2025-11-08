# Contributing

This TAM emulator is deliberately open source and I'm happy for anyone with
the expertise to fix bugs or ideas for new features to help maintain and improve
this project!

## Giving feedback

### Reporting bugs

Submit bugs using Github's issue tracking. Use the relevant issue templates
where possible to make them easier to read.

### Suggesting enhancements

I'd love to hear ideas for any new features you'd find helpful. Use the issue
tracker with the 'Feature request' template to suggest it.

## Making contributions

### Coding conventions

We follow [Google's C++ style guide](https://google.github.io/styleguide/cppguide.html),
with the exception that tabs should be four spaces wide. There is a `.clang-format`
file in the root which should make ensure your files are automatically
in the right style.

### Testing

All changes should have relevant tests written for them.
Tests are written using [GoogleTest](https://google.github.io/googletest/primer.html)
and are contained in the top-level `test` directory. Refer to the GoogleTest
documentation for details on how to write tests.

### Submitting changes

This repository uses a fork-pull request method for changes. To implement changes
please fork this project, do your work in your own fork, and then submit a pull
request for review.

