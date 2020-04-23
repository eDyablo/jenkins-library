package com.e4d.declaration

import hudson.model.AbstractItem

class AbstractItemDeclaration extends Declaration {
  final AbstractItem item

  AbstractItemDeclaration(AbstractItem item) {
    this.item = item
  }

  void displayName(String name) {
    item.setDisplayName(name?.trim())
  }

  void description(String description) {
    item.setDescription(description?.stripIndent()?.trim())
  }
}
