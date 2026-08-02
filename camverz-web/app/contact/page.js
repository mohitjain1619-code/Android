'use client';
import { useState } from 'react';
import { Mail, MessageSquare, Phone, MapPin, Send, CheckCircle, FileText } from 'lucide-react';
import styles from './page.module.css';

export default function ContactPage() {
  const [formData, setFormData] = useState({ name: '', email: '', subject: '', message: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.email || !formData.message) {
      alert('Please fill out all required fields.');
      return;
    }
    setIsSubmitting(true);
    // Simulate submission
    await new Promise((resolve) => setTimeout(resolve, 1500));
    setIsSubmitting(false);
    setSubmitted(true);
    setFormData({ name: '', email: '', subject: '', message: '' });
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Header */}
        <header className={styles.header}>
          <h1 className="neon-text">Contact Support</h1>
          <p>Have questions, suggestions, or need help? Reach out to the Camverz team.</p>
        </header>

        <div className={styles.layout}>
          
          {/* Contact Details */}
          <div className={styles.detailsColumn}>
            <div className={styles.infoCard}>
              <h2>Get in Touch</h2>
              <p className={styles.infoText}>
                We usually respond to support tickets and queries within 12–24 hours. Send us a message!
              </p>

              <div className={styles.infoList}>
                <div className={styles.infoItem}>
                  <div className={styles.iconBox}>
                    <Mail size={20} />
                  </div>
                  <div>
                    <span>Email Us</span>
                    <strong>support@camverz.com</strong>
                  </div>
                </div>

                <div className={styles.infoItem}>
                  <div className={styles.iconBox}>
                    <MessageSquare size={20} />
                  </div>
                  <div>
                    <span>Live Chat</span>
                    <strong>Available inside the mobile app</strong>
                  </div>
                </div>

                <div className={styles.infoItem}>
                  <div className={styles.iconBox}>
                    <MapPin size={20} />
                  </div>
                  <div>
                    <span>Office Location</span>
                    <strong>New Delhi, India</strong>
                  </div>
                </div>

                <div className={styles.infoItem}>
                  <div className={styles.iconBox}>
                    <FileText size={20} />
                  </div>
                  <div>
                    <span>Merchant Name</span>
                    <strong>Mohit Jain</strong>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Contact Form */}
          <div className={styles.formColumn}>
            <div className={styles.formCard}>
              {submitted ? (
                <div className={styles.successState}>
                  <CheckCircle size={48} className={styles.successIcon} />
                  <h2>Message Sent!</h2>
                  <p>Thank you for reaching out. We have received your message and our team will get back to you shortly.</p>
                  <button className="btn-glass" onClick={() => setSubmitted(false)}>
                    Send Another Message
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className={styles.form}>
                  <div className={styles.formRow}>
                    <div className={styles.inputGroup}>
                      <label htmlFor="name">Your Name *</label>
                      <input
                        type="text"
                        id="name"
                        name="name"
                        className="input-glass"
                        placeholder="John Doe"
                        value={formData.name}
                        onChange={handleChange}
                        required
                        disabled={isSubmitting}
                      />
                    </div>
                    <div className={styles.inputGroup}>
                      <label htmlFor="email">Your Email *</label>
                      <input
                        type="email"
                        id="email"
                        name="email"
                        className="input-glass"
                        placeholder="john@example.com"
                        value={formData.email}
                        onChange={handleChange}
                        required
                        disabled={isSubmitting}
                      />
                    </div>
                  </div>

                  <div className={styles.inputGroup}>
                    <label htmlFor="subject">Subject</label>
                    <input
                      type="text"
                      id="subject"
                      name="subject"
                      className="input-glass"
                      placeholder="How can we help?"
                      value={formData.subject}
                      onChange={handleChange}
                      disabled={isSubmitting}
                    />
                  </div>

                  <div className={styles.inputGroup}>
                    <label htmlFor="message">Message *</label>
                    <textarea
                      id="message"
                      name="message"
                      className={`${styles.textarea} input-glass`}
                      placeholder="Type your message here..."
                      value={formData.message}
                      onChange={handleChange}
                      required
                      disabled={isSubmitting}
                    />
                  </div>

                  <button
                    type="submit"
                    className="btn-neon"
                    style={{ width: '100%', marginTop: '12px' }}
                    disabled={isSubmitting}
                  >
                    <Send size={16} /> {isSubmitting ? 'Sending...' : 'Send Message'}
                  </button>
                </form>
              )}
            </div>
          </div>

        </div>

      </div>
    </div>
  );
}
