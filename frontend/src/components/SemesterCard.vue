<script setup>
defineProps({
  semester: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <article class="semester-card">
    <header class="semester-header">
      <div>
        <p class="eyebrow">Semester {{ semester.year }}</p>
        <h3>{{ semester.term.charAt(0) + semester.term.slice(1).toLowerCase() }}</h3>
      </div>
      <span class="credit-badge">{{ semester.totalCredits }} credits</span>
    </header>

    <ul class="semester-courses">
      <li v-for="course in semester.courses" :key="course.id">
        <span class="course-code">{{ course.id }}</span>
        <span class="course-title">{{ course.name }}</span>
        <span class="course-credit">{{ course.credits }} cr</span>
      </li>
    </ul>
  </article>
</template>
