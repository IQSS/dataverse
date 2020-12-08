Consuming Configuration
=======================

By using `MicroProfile Config <https://github.com/eclipse/microprofile-config>`_, we start to move away from
direct ``System.getProperty()`` calls, replacing it with either injected or programmatic config retrieval.

The API of MPCONFIG allows for great flexibility, type-safety, defaults etc. For system administrators
and developers it get's much easier to provide their configuration.

This technology is introduced on a step-by-step basis. There will not be a big shot, crashing upgrades for everyone.
Instead, we will provide backward compatibility by deprecating renamed or moved config options, while still
supporting the old way of setting them.