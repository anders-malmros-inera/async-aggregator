# Repository-Wide Refactoring Plan

**Date:** November 18, 2025  
**Status:** Planning Phase  
**Repository:** async-aggregator

## Executive Summary

Following the successful refactoring of the aggregator module (SseService, AggregatorService), this document outlines opportunities for continuous improvement across the remaining modules: `resource` and `client`.

---

## Module Analysis

### 1. Resource Module

**Current State:**
- Simple Spring WebFlux service that simulates resource callbacks
- 6 Java source files
- No test coverage currently
- Clean, focused controller
- Service class has some complexity in note generation

**Refactoring Opportunities:**

#### High Priority
1. **Add Unit Tests for ResourceService**
   - Test callback sending logic
   - Test error handling (negative delays)
   - Test note generation
   - **Estimated effort:** 2-3 hours
   - **Risk:** Low
   - **Value:** High (prevents regressions)

2. **Extract Note Generation Logic**
   - Create `JournalNoteGenerator` class
   - Separate concerns: business logic from test data generation
   - Makes testing easier
   - **Estimated effort:** 1 hour
   - **Risk:** Low
   - **Value:** Medium (improves testability)

#### Medium Priority
3. **Add Integration Test**
   - Test full request/response cycle
   - Verify WebClient configuration
   - **Estimated effort:** 2 hours
   - **Risk:** Low (may encounter same containerization issues as aggregator)
   - **Value:** Medium

4. **Configuration Validation**
   - Add `@NotNull` or validation for `resource.id` property
   - Fail fast if misconfigured
   - **Estimated effort:** 30 minutes
   - **Risk:** None
   - **Value:** Low (nice-to-have)

#### Low Priority
5. **Improve Error Handling**
   - Add retry logic for callback posting
   - More detailed error messages
   - **Estimated effort:** 1-2 hours
   - **Risk:** Low
   - **Value:** Low (current error handling is adequate)

**Code Quality Metrics:**
- ✅ Single Responsibility Principle: Good
- ✅ Dependency Injection: Proper
- ⚠️ Test Coverage: **0%** - Needs improvement
- ✅ Complexity: Low
- ✅ Duplication: None detected

---

### 2. Client Module

**Current State:**
- Simple Thymeleaf-based web UI
- 2 Java source files (minimal backend)
- No test coverage
- Most logic in frontend JavaScript
- Recently cleaned WebRTC references

**Refactoring Opportunities:**

#### High Priority
1. **Add Controller Tests**
   - Test endpoint availability
   - Verify template resolution
   - **Estimated effort:** 1 hour
   - **Risk:** Low
   - **Value:** Medium (ensures endpoint stability)

#### Medium Priority
2. **Frontend Code Organization**
   - Already well-structured with:
     - `aggregatorClient.js` - API interaction
     - `sseClient.js` - SSE handling
     - `uiController.js` - DOM manipulation
   - Consider adding JSDoc comments
   - **Estimated effort:** 2 hours
   - **Risk:** None
   - **Value:** Low (current structure is good)

3. **Add Frontend Unit Tests**
   - Use Jest or similar for JavaScript testing
   - Test UI logic, SSE handling
   - **Estimated effort:** 4-6 hours (setup + tests)
   - **Risk:** Medium (requires tooling setup)
   - **Value:** Medium

**Code Quality Metrics:**
- ✅ Single Responsibility: Excellent (minimal backend, clear frontend separation)
- ✅ Complexity: Very Low
- ⚠️ Test Coverage: **0%** (backend), **0%** (frontend)
- ✅ Recent Cleanup: WebRTC removed successfully

---

## Recommended Implementation Order

### Phase 1: Testing Foundation (Week 1)
**Goal:** Establish test coverage for all modules

1. ✅ **Aggregator Module** - COMPLETE
   - SseService unit tests ✓
   - Integration test skeleton ✓
   
2. **Resource Module**
   - Add ResourceService unit tests
   - Create `JournalNoteGenerator` for testability
   - Add basic controller test

3. **Client Module**
   - Add ClientController test
   - Verify template rendering

**Deliverable:** All modules have >80% backend test coverage

### Phase 2: Code Quality (Week 2)
**Goal:** Apply clean code patterns consistently

1. **Resource Module**
   - Extract note generation to separate class
   - Add configuration validation
   - Document public APIs with Javadoc

2. **Client Module**
   - Add JSDoc to frontend code
   - Consider frontend testing framework (optional)

3. **Documentation**
   - Update README with architecture diagrams
   - Document SSE flow and timing considerations
   - Add troubleshooting guide

**Deliverable:** Consistent code quality across all modules

### Phase 3: Advanced Topics (Future)
**Goal:** Production hardening

1. **Monitoring & Observability**
   - Add Micrometer metrics
   - Implement distributed tracing
   - Health check endpoints

2. **Resilience**
   - Circuit breakers for external calls
   - Retry policies
   - Timeouts and bulkheads

3. **Performance**
   - Load testing
   - Profile SSE under high concurrency
   - Optimize resource consumption

**Deliverable:** Production-ready microservices

---

## Proposed Test Coverage Targets

| Module | Current | Target (Phase 1) | Target (Phase 2) |
|--------|---------|------------------|------------------|
| Aggregator | ~85% | 85% ✓ | 90% |
| Resource | 0% | 80% | 85% |
| Client (Backend) | 0% | 70% | 75% |
| Client (Frontend) | 0% | N/A | 50% (optional) |

---

## Risk Assessment

### Low Risk Changes
- Adding unit tests (no behavior change)
- Extracting pure functions
- Documentation updates
- Adding validation

### Medium Risk Changes
- Integration tests (timing/environment issues)
- Refactoring with concurrent code
- Frontend test framework setup

### High Risk Changes
- None identified in current scope

---

## Success Criteria

### Phase 1 Complete When:
- [ ] ResourceService has unit tests with >80% coverage
- [ ] Resource note generation is extracted to separate class
- [ ] ClientController has basic tests
- [ ] All tests pass in Docker/CI environment
- [ ] Build time remains under 60 seconds

### Phase 2 Complete When:
- [ ] All public methods have Javadoc
- [ ] README includes architecture diagrams
- [ ] Code review checklist created and applied
- [ ] No critical SonarQube/linting violations

---

## Next Steps

1. **Immediate (This Session):**
   - Create ResourceService unit tests
   - Extract JournalNoteGenerator class
   - Add basic ClientController test

2. **Short Term (Next Session):**
   - Run full test suite
   - Verify all modules build successfully
   - Update documentation

3. **Long Term:**
   - Proceed to Phase 2 based on team priorities
   - Consider CI/CD pipeline improvements

---

## Notes

- All changes should be made in small, atomic commits
- Each PR should include tests
- Maintain backward compatibility
- Update this document as work progresses
- Current refactoring patterns from aggregator module serve as template

---

## References

- [Clean Code Principles](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)
- [Spring WebFlux Testing Guide](https://docs.spring.io/spring-framework/reference/testing/webtestclient.html)
- [Project Reactor Testing](https://projectreactor.io/docs/core/release/reference/#testing)
