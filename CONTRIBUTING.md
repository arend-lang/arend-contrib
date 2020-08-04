# Code style

+ Use intellij-arend formatter unless you are very sure what you're doing.

# Naming convention

(thanks to [HoTT-Agda])

+ Capitalize names of types (including type parameters), classes, instances, and
  people or place or whatever real-world names.
+ Capitalize package and module names, reuse Haskell and [HoTT-Coq] names if possible.
+ Do not capitalize other words, like `generalized` or `contr`.
+ Use symbols if appropriate, like `>=`, `>=>`, etc. Try to be ligature-friendly.
+ Names should be in hyphen-separated-lowercase-words.
+ If there is a very common phrase, camelCase it (like `BinaryTree`, `mergeSort`).
+ Use `->` to replace `to`, `=` to replace synonyms of "having a path",
  and `~=` to replace synonyms of "being isomorphic" if possible.
+ Use `isA` to replace `is-a` and `is-A` if possible, same for `has`.
  + In case `A` is actually multiple words, use camelCase.
+ If you are very strong on a nonstandard name and have clear reasons, you can
  comment it and it might be allowed.
+ Use commonly accepted shorthands, such as (complete this list when you use a new one):
  + `-inj` and `-surj` for injectivity and surjectivity.
  + `-nat` for naturality.
  + `-equiv` -- when you're looking for this, use infix `=` or `~=` instead.
  + `-comm` for commutativity.
  + `-assoc` for associativity.
  + `-dist` for distributivity.
  + `-trans` for transitivity.
  + `-sym` for symmetry.

 [HoTT-Coq]: https://github.com/HoTT/HoTT
 [HoTT-Agda]: https://github.com/HoTT/HoTT-Agda

Examples:

```arend
A~=B : Iso A B
A=B : A = B
A=B->C=D : A = B -> C = D
```

## Properties

Names of the form `isX` or `hasX`, represent properties that can hold (or not)
for some type `A`. Such a property can be parametrized by some arguments. The
property is said to hold for a type `A` iff `isX args A` is inhabited. The
types `isX args A` should be (h-)propositions.

Example:

```arend
isContr
isProp
hasLevel    -- This one has one argument of type [ℕ₋₂]
hasAllPaths -- Every two points are equal
hasDecEq    -- Decidable equality
```

- The theorem stating that some type `A` (perhaps with arguments) has some
  property `isX` is named `A-isX`. The arguments of `A-isX` are the arguments
  of `isX` followed by the arguments of `A`.
- Theorems stating that any type satisfying `isX` also satisfies `isY` are
  named `isX->isY` (and not `isX-isY` which would mean `isY (isX A)`).

```arend
Unit-isContr : isContr Unit
Bool-isSet : isSet Bool
isContr-isProp : isContr (isProp A)
isContr->isProp : isContr A -> isProp A
decEq->isSet : hasDecEq A -> isSet A
isContr->hasAllPaths : isContr A -> hasAllPaths A
```
