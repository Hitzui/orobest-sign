using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Linq;
using System.Reflection;
using System.Threading;
using NTwain;
using NTwain.Data;

[assembly: AssemblyTitle("Orobest")]
[assembly: AssemblyCompany("SEIN")]
[assembly: AssemblyProduct("Orobest")]
[assembly: AssemblyVersion("1.0.0.0")]

namespace Orobest.Scanner
{
    public sealed class Device
    {
        public string id;
        public string name;
    }

    public sealed class Result
    {
        public string status = "ok";
        public string code;
        public List<Device> devices = new List<Device>();
    }

    public static class TwainBridge
    {
        public static Result Run(string action, string deviceName, string outputPath)
        {
            // Epson's network sources are often 32-bit even on 64-bit Windows.
            // The caller selects an isolated x86/x64 PowerShell process accordingly.
            PlatformInfo.Current.PreferNewDSM = Environment.Is64BitProcess;
            // PowerShell compiles an in-memory assembly, which has no file path
            // for TWIdentity.CreateFromAssembly/FileVersionInfo to inspect.
            var identity = new TWIdentity {
                DataGroup = DataGroups.Control | DataGroups.Image,
                ProtocolMajor = 2, ProtocolMinor = 3,
                Manufacturer = "SEIN", ProductFamily = "Orobest", ProductName = "Orobest",
                Version = new TWVersion { Major = 1, Minor = 0, Info = "Orobest scanner bridge" }
            };
            var session = new TwainSession(identity);
            try
            {
                if (session.Open() != ReturnCode.Success)
                    return Error("twain_unavailable");
                var sources = session.ToList();
                if (action == "List")
                {
                    var result = new Result();
                    foreach (var source in sources)
                        result.devices.Add(new Device { id = source.Name, name = source.Name });
                    return result;
                }
                var selected = sources.FirstOrDefault(s => string.Equals(s.Name, deviceName, StringComparison.Ordinal));
                if (selected == null) return Error("device_not_found");
                if (selected.Open() != ReturnCode.Success) return Error("twain_open_failed");
                return Acquire(session, selected, outputPath);
            }
            catch
            {
                return Error("scanner_error");
            }
            finally
            {
                // Never close a source from a transfer event. Unwind on this thread.
                try { if (session.IsDsmOpen) session.ForceStepDown(3); } catch { }
                try { if (session.IsDsmOpen) session.Close(); } catch { }
            }
        }

        private static Result Acquire(TwainSession session, DataSource source, string path)
        {
            bool saved = false;
            string failure = null;
            using (var completed = new ManualResetEvent(false))
            {
                session.StopOnTransferError = true;
                EventHandler<TransferReadyEventArgs> ready = (sender, args) => { if (saved || failure != null) args.CancelAll = true; };
                EventHandler<DataTransferredEventArgs> transferred = (sender, args) =>
                {
                    if (saved || failure != null) return;
                    try
                    {
                        using (var stream = args.GetNativeImageStream())
                        {
                            if (stream == null) throw new InvalidOperationException();
                            using (var image = Image.FromStream(stream)) image.Save(path, ImageFormat.Bmp);
                        }
                        saved = true;
                    }
                    catch { failure = "invalid_image"; }
                };
                EventHandler<TransferErrorEventArgs> error = (sender, args) => { failure = "scanner_error"; completed.Set(); };
                EventHandler disabled = (sender, args) => completed.Set();
                session.TransferReady += ready;
                session.DataTransferred += transferred;
                session.TransferError += error;
                session.SourceDisabled += disabled;

                try
                {
                    // Optional capabilities may be absent or read-only on a driver.
                    SetOptional(() => { if (source.Capabilities.CapFeederEnabled.CanSet) source.Capabilities.CapFeederEnabled.SetValue(BoolType.False); });
                    SetOptional(() => { if (source.Capabilities.CapDuplexEnabled.CanSet) source.Capabilities.CapDuplexEnabled.SetValue(BoolType.False); });
                    SetOptional(() => { if (source.Capabilities.CapXferCount.CanSet) source.Capabilities.CapXferCount.SetValue(1); });
                    SetOptional(() => { if (source.Capabilities.ICapPixelType.CanSet) source.Capabilities.ICapPixelType.SetValue(PixelType.RGB); });
                    SetOptional(() => { if (source.Capabilities.ICapXResolution.CanSet) source.Capabilities.ICapXResolution.SetValue((TWFix32)300f); });
                    SetOptional(() => { if (source.Capabilities.ICapYResolution.CanSet) source.Capabilities.ICapYResolution.SetValue((TWFix32)300f); });
                    // Native transfer is required by GetNativeImageStream().
                    if (source.Capabilities.ICapXferMech.CanSet && source.Capabilities.ICapXferMech.SetValue(XferMech.Native) != ReturnCode.Success)
                        return Error("unsupported_transfer");
                    if (source.Capabilities.ICapXferMech.GetCurrent() != XferMech.Native)
                        return Error("unsupported_transfer");

                    var rc = source.Enable(SourceEnableMode.NoUI, false, IntPtr.Zero);
                    if (rc != ReturnCode.Success && !session.IsSourceEnabled)
                        rc = source.Enable(SourceEnableMode.ShowUI, false, IntPtr.Zero);
                    if (rc != ReturnCode.Success) return Error("twain_open_failed");
                    if (!completed.WaitOne(TimeSpan.FromSeconds(280))) return Error("scanner_timeout");
                    if (failure != null) return Error(failure);
                    return saved ? new Result() : new Result { status = "cancelled" };
                }
                finally
                {
                    session.TransferReady -= ready;
                    session.DataTransferred -= transferred;
                    session.TransferError -= error;
                    session.SourceDisabled -= disabled;
                }
            }
        }

        private static void SetOptional(Action action) { try { action(); } catch { } }
        private static Result Error(string code) { return new Result { status = "error", code = code }; }
    }
}
