package com.classicaltiles.game

/**
 * Short, simplified single-note game arrangements of public-domain compositions.
 * The app renders every note with its own lightweight synthesized piano voice.
 * These are gameplay adaptations, not commercial recordings or modern score editions.
 */
data class Song(
    val id: String,
    val title: String,
    val composer: String,
    val bpm: Int,
    val notes: IntArray,
    val note: String
)

object ClassicalTracks {
    private fun n(vararg notes: Int) = notes

    val all: List<Song> = listOf(
        Song(
            "bach_prelude", "Prelude in C Major, BWV 846", "J. S. Bach", 112,
            n(60,64,67,72,67,64,60,64,67,72,67,64,59,62,67,71,67,62,60,64,67,72,67,64,60,64,67,72,67,64,59,62,67,71,67,62,60,64,67,72,76,72,67,64,60,64,67,72,67,64),
            "Baroque"
        ),
        Song(
            "minuet_g", "Minuet in G Major, BWV Anh. 114", "C. Petzold (traditionally attributed to Bach)", 116,
            n(74,67,69,71,72,74,67,67,69,71,72,74,67,67,76,74,72,71,69,67,69,71,72,74,76,74,72,71,72,67,67,74,67,69,71,72,74,67,67,69,71,72,74,67,67,76,74,72,71),
            "Baroque"
        ),
        Song(
            "jesu_joy", "Jesu, Joy of Man's Desiring", "J. S. Bach", 104,
            n(67,71,74,79,78,76,74,72,71,69,67,69,71,72,74,71,67,71,74,79,78,76,74,72,71,69,67,69,71,72,74,76,74,72,71,69,67,69,71,74,72,71,69,67,64,67,71,74),
            "Cantata"
        ),
        Song(
            "air_g_string", "Air (Orchestral Suite No. 3)", "J. S. Bach", 88,
            n(74,73,74,78,81,78,74,73,71,69,67,66,64,62,64,66,67,69,71,74,73,71,69,67,74,73,74,78,81,78,74,73,71,69,67,66,64,62,64,66,67,69,71,74,76,74,71,69),
            "Orchestral Suite"
        ),
        Song(
            "turkish_march", "Rondo Alla Turca (Turkish March)", "W. A. Mozart", 136,
            n(71,70,71,72,74,72,71,69,68,69,71,72,71,69,67,66,67,69,71,72,74,76,74,72,71,69,68,69,71,72,74,71,69,67,66,67,69,71,72,74,76,78,76,74,72,71,69,68),
            "Classical"
        ),
        Song(
            "eine_kleine", "Eine kleine Nachtmusik", "W. A. Mozart", 128,
            n(67,74,67,74,67,74,71,74,72,76,72,76,72,76,69,76,67,74,67,74,67,74,71,74,72,76,72,76,72,76,69,76,74,79,76,74,72,71,72,74,76,79,81,79,76,74,72,71),
            "Serenade"
        ),
        Song(
            "k545", "Piano Sonata No. 16 in C, K. 545", "W. A. Mozart", 120,
            n(60,67,64,67,72,74,76,77,79,79,79,81,79,77,76,74,72,67,64,67,72,74,76,77,79,79,79,81,79,77,76,74,72,71,69,67,65,64,62,60,62,64,65,67,69,71,72,74),
            "Classical"
        ),
        Song(
            "lacrimosa", "Lacrimosa (Requiem in D minor)", "W. A. Mozart", 78,
            n(62,62,62,65,64,62,61,62,65,64,62,61,62,67,65,64,62,60,62,65,67,69,67,65,64,62,61,62,65,64,62,61,62,67,65,64,62,60,62,65,67,69,71,69,67,65,64,62),
            "Requiem"
        ),
        Song(
            "fur_elise", "Für Elise, WoO 59", "L. van Beethoven", 120,
            n(76,75,76,75,76,71,74,72,69,60,64,69,71,64,68,71,72,64,76,75,76,75,76,71,74,72,69,60,64,69,71,64,72,71,69,67,65,67,69,71,72,74,76,76,75,76,75,76,71),
            "Bagatelle"
        ),
        Song(
            "ode_to_joy", "Ode to Joy (Symphony No. 9)", "L. van Beethoven", 116,
            n(64,64,65,67,67,65,64,62,60,60,62,64,64,62,62,64,64,65,67,67,65,64,62,60,60,62,64,62,60,60,62,62,64,60,62,64,65,64,60,62,64,65,64,62,60,62,55,60),
            "Symphony"
        ),
        Song(
            "moonlight", "Moonlight Sonata (I. Adagio sostenuto)", "L. van Beethoven", 92,
            n(56,61,64,68,61,64,68,61,56,61,64,68,61,64,68,61,57,61,64,69,61,64,69,61,56,61,64,68,61,64,68,61,54,61,64,69,61,64,69,61,56,61,64,68,61,64,68,61),
            "Piano Sonata"
        ),
        Song(
            "symphony5", "Symphony No. 5 (opening motif)", "L. van Beethoven", 144,
            n(67,67,67,63,65,65,65,62,67,67,67,63,65,65,65,62,67,67,67,63,65,65,65,62,67,70,69,65,67,70,69,65,67,67,67,63,65,65,65,62,69,69,69,65,67,67,67,63),
            "Symphony"
        ),
        Song(
            "chopin_prelude", "Prelude in E Minor, Op. 28 No. 4", "F. Chopin", 82,
            n(76,75,76,79,78,76,75,73,71,69,68,66,64,66,68,69,71,73,75,76,75,73,71,69,68,66,64,66,68,69,71,73,76,75,76,79,78,76,75,73,71,69,68,66,64,66,68,64),
            "Romantic"
        ),
        Song(
            "pachelbel_canon", "Canon in D", "J. Pachelbel", 108,
            n(78,76,74,73,71,69,71,73,74,69,71,73,74,76,78,76,74,73,71,69,71,73,74,69,71,73,74,76,78,81,79,78,76,74,73,71,69,71,73,74,76,78,76,74,73,71,69,71),
            "Baroque"
        ),
        Song(
            "vivaldi_spring", "Spring (The Four Seasons), I. Allegro", "A. Vivaldi", 132,
            n(76,76,76,72,74,74,74,71,72,72,72,69,71,71,71,67,76,76,76,72,74,74,74,71,79,77,76,74,72,71,69,67,76,76,76,72,74,74,74,71,72,72,72,69,71,74,72,71),
            "Concerto"
        )
    )
}
