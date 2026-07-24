'use client';
import { use, useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Calendar, Clock, ArrowLeft, Shield, Video, Heart, AlertTriangle } from 'lucide-react';
import styles from './page.module.css';

const articlesContent = {
  'safe-video-calling-tips': {
    title: '5 Tips for Safe Video Calling with Strangers',
    category: 'Safety',
    date: 'June 20, 2026',
    readTime: '4 min read',
    icon: '🔒',
    content: (
      <>
        <p>Video calling platforms like Camverz are incredible tools to meet diverse people from all over the world. But talking to strangers requires sound judgment and general safety awareness. By keeping these five rules in mind, you can have a fun, positive, and safe experience.</p>
        
        <h2>1. Protect Your Personal Information</h2>
        <p>Never share your full name, physical address, phone number, email address, or social media links in the first few minutes of a conversation. Fraudsters can use small details to locate your profile or build a profile of your identity. Wait until you have established a high level of trust before sharing outside contacts.</p>

        <h2>2. Mind Your Surroundings</h2>
        <p>Before hitting that "Start Call" button, look around your room. Is there a school diploma, mail package, or family photo in the background? Turn on appropriate lighting or use neutral backgrounds to make sure you do not reveal your exact location or private details about your life.</p>

        <h2>3. Use the Report and Block Button</h2>
        <p>If another user behaves inappropriately, makes offensive remarks, or acts suspicously, do not engage. Camverz has a built-in <strong>Report</strong> and <strong>Block</strong> system. Reporting a bad actor helps our community moderators clean up the environment and protect other users.</p>

        <blockquote>
          <strong>Pro-Tip:</strong> All reports are processed within 15 minutes by our moderation queue. Verified violations result in a permanent hardware ban for the offending user.
        </blockquote>

        <h2>4. Keep It Clean and Respectful</h2>
        <p>Respect boundaries and treat others the way you want to be treated. Do not ask for inappropriate actions, and do not perform any. Our platform enforces strict standards of conduct to keep the community healthy.</p>

        <h2>5. Don’t Let Anyone Pressure You</h2>
        <p>If a call makes you uncomfortable at any point, remember that you have complete control. You can end the call immediately. You do not owe anyone a conversation, an explanation, or a second of your time.</p>
      </>
    )
  },
  'video-chat-online-dating': {
    title: 'How Random Video Chat is Changing Online Dating',
    category: 'Dating',
    date: 'June 18, 2026',
    readTime: '6 min read',
    icon: '💑',
    content: (
      <>
        <p>For the past decade, online dating has been dominated by a single mechanic: swiping left or right on a series of heavily filtered photos and text bios. While this changed dating access, it has also led to dating fatigue, ghosting, and catfishing. Enter random video chat.</p>

        <h2>The Death of Catfishing</h2>
        <p>A static image can lie, but live video cannot. By matching with people live, you immediately see the person as they truly are. There are no angles, old photos, or filter tricks. You know from the first second who you are talking to, which brings authenticity back to the dating process.</p>

        <h2>The Spark Test: Micro-Expressions</h2>
        <p>Dating apps often result in weeks of texting, only to meet in person and realize there is absolutely no chemistry. Video calling captures voice, tone, laughter, and micro-expressions. You can tell in two minutes of live chatting if a spark exists, saving hours of messaging.</p>

        <h2>Breaking Geographical Barriers</h2>
        <p>Traditional dating apps limit you to a narrow local search radius. Random video call apps expand your horizons globally. You could be chatting with a student in Milan, an artist in Tokyo, or a software engineer in San Francisco—expanding your perspective and dating options globally.</p>

        <h2>Safety First in Digital Dating</h2>
        <p>Meeting on a random video platform means you don't have to share your phone number or coordinates to see someone face-to-face. It acts as a safe, neutral space to test the waters before taking any steps further.</p>
      </>
    )
  },
  'genuine-connections-digital-age': {
    title: 'Building Genuine Connections in the Digital Age',
    category: 'Relationships',
    date: 'June 15, 2026',
    readTime: '5 min read',
    icon: '✨',
    content: (
      <>
        <p>With smartphones and notifications continually bidding for our attention, deep connections are harder to find. When you start a random video call, you have a unique opportunity to build a brief, yet meaningful connection. Here is how you can do it.</p>

        <h2>Practice Active Listening</h2>
        <p>Do not just wait for your turn to speak. Pay attention to what the other person is saying, their tone of voice, and body language. Nodding, smiling, and acknowledging their points shows you are genuinely engaged in the conversation.</p>

        <h2>Ask Open-Ended Questions</h2>
        <p>Avoid boring questions like "How are you?" or "Where are you from?". Instead, ask questions that invite story-telling.
          Examples:
          <ul>
            <li>"What is the most interesting thing that happened to you today?"</li>
            <li>"If you could travel anywhere tomorrow, where would you go?"</li>
            <li>"What is a passion project you are working on?"</li>
          </ul>
        </p>

        <h2>Be Authentic</h2>
        <p>Do not try to play a character. Share your real thoughts, hobbies, and interests. Authenticity is magnetic. When you show your true self, the other person is much more likely to open up and connect on a real level.</p>

        <h2>Embrace the Silence</h2>
        <p>A short silence during a conversation does not have to be awkward. Use it to smile, collect your thoughts, or ask about something in their frame. Comfortable pauses are signs of natural interactions.</p>
      </>
    )
  },
  'gender-verification-importance': {
    title: 'Understanding Gender Verification: Why It Matters',
    category: 'Community',
    date: 'June 10, 2026',
    readTime: '3 min read',
    icon: '🛡️',
    content: (
      <>
        <p>In online spaces, trust is everything. For random video chat platforms, keeping a balanced and authentic community is a major challenge. At Camverz, we solve this issue with our advanced Gender Verification system. Here is why this feature matters for safety and fun.</p>

        <h2>Balancing the Community</h2>
        <p>Many video chat networks suffer from extreme gender imbalances, leading to a frustrating experience. Verification helps maintain a healthy balance so everyone finds matches quickly without spam.</p>

        <h2>Deterring Bad Actors</h2>
        <p>When users know that profiles must undergo verification, bad actors are discouraged. By requiring female profiles to verify via active photos, we ensure that you are matching with real people, preventing bots or fake accounts.</p>

        <h2>How Camverz Verification Works</h2>
        <p>Our verification uses simple, secure steps:
          <ol>
            <li>Select your gender during onboarding.</li>
            <li>If female, take a quick selfie matching a random pose gesture.</li>
            <li>Our secure systems confirm the match in real-time.</li>
            <li>A blue checkmark is placed on verified profiles.</li>
          </ol>
        </p>
        <p>This verification process is designed to protect your privacy. Your selfie is never shared on your profile or sold to third parties; it is solely used for authentication.</p>
      </>
    )
  },
  'long-distance-relationships-tips': {
    title: 'Long-Distance Relationships: Making Them Work',
    category: 'Relationships',
    date: 'June 05, 2026',
    readTime: '7 min read',
    icon: '✈️',
    content: (
      <>
        <p>Met someone special on Camverz who lives miles away? Long-distance relationships (LDRs) can be challenging, but they can also build incredibly strong emotional bonds. With the right communication strategies, distance is just a number.</p>

        <h2>Schedule Routine "Video Dates"</h2>
        <p>Do not just check in with short texts. Plan dedicated dates where you both dress up, order the same type of food, and sit down for an hour-long video call. Treat it with the same respect as an in-person date.</p>

        <h2>Share Everyday Activities</h2>
        <p>You do not always need to hold active conversations. Set up your phone or laptop while you cook dinner, study, or watch a movie together. Sharing quiet, ordinary moments builds a strong sense of companionship.</p>

        <h2>Establish Clear Expectations</h2>
        <p>Talk openly about your goals, schedules, and when you plan to meet in person. Having a clear plan and mutual understanding helps reduce uncertainty and keeps both partners aligned.</p>

        <h2>Keep the Trust Alive</h2>
        <p>Distance can amplify insecurity. Practice open honesty, share your day-to-day schedule, and avoid hiding small details. Mutual trust is the absolute anchor of a successful long-distance connection.</p>
      </>
    )
  },
  'online-safety-privacy-guide': {
    title: 'Online Safety Guide: Protecting Your Privacy',
    category: 'Safety',
    date: 'May 28, 2026',
    readTime: '8 min read',
    icon: '🛡️',
    content: (
      <>
        <p>Privacy is a fundamental right. When chatting online, taking steps to secure your personal data is essential. This guide outlines how Camverz protects your data, and how you can manage your digital footprint safely.</p>

        <h2>Zero-Recording Policy</h2>
        <p>At Camverz, your video streams are strictly peer-to-peer and encrypted. We do not store, monitor, or record your video call data. What happens on a call stays between you and your match.</p>

        <h2>Watch out for Social Engineering</h2>
        <p>Be careful if someone tries to direct you to external messaging services or claims to need help or money. Always keep interactions within the app until you are absolutely certain of their identity.</p>

        <h2>Secure Your Account</h2>
        <p>Ensure your account is protected by using secure Google authentication. Do not share your login credentials with anyone, and log out when using public or shared computers.</p>

        <blockquote>
          <p><strong>Remember:</strong> Camverz administrators will never contact you asking for your password, verification codes, or billing details. If you receive such messages, report them immediately.</p>
        </blockquote>
      </>
    )
  }
};

export default function BlogDetail({ params }) {
  const router = useRouter();
  
  // Resolve params using React.use() wrapper to satisfy React 19 rules in Next.js
  const resolvedParams = use(params);
  const slug = resolvedParams.slug;
  
  const article = articlesContent[slug];

  if (!article) {
    return (
      <div className={styles.notFoundContainer}>
        <div className={styles.bgGradientPage} />
        <div className={styles.notFoundContent}>
          <AlertTriangle size={48} className={styles.warnIcon} />
          <h2>Article Not Found</h2>
          <p>The blog post you are looking for does not exist or has been removed.</p>
          <Link href="/blog" className="btn-neon">
            <ArrowLeft size={16} /> Back to Blog
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Back Link */}
        <Link href="/blog" className={styles.backLink}>
          <ArrowLeft size={16} />
          <span>Back to Blog</span>
        </Link>

        {/* Article Layout */}
        <article className={styles.articleCard}>
          <header className={styles.header}>
            <div className={styles.emojiHero}>{article.icon}</div>
            <span className={styles.categoryBadge}>{article.category}</span>
            <h1>{article.title}</h1>
            <div className={styles.metaRow}>
              <span className={styles.metaItem}>
                <Calendar size={14} /> {article.date}
              </span>
              <span className={styles.metaItem}>
                <Clock size={14} /> {article.readTime}
              </span>
            </div>
          </header>

          <hr className={styles.divider} />

          <div className={styles.content}>
            {article.content}
          </div>

          <footer className={styles.footer}>
            <h3>Ready to connect?</h3>
            <p>Start matching with real users safely on Camverz. Find friends, relationships, or casual conversations instantly.</p>
            <button className="btn-neon" onClick={() => router.push('/')}>
              <Video size={18} /> Start Video Calling
            </button>
          </footer>
        </article>

      </div>
    </div>
  );
}
